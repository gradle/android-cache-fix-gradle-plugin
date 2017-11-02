package org.gradle.android

import com.android.build.gradle.internal.Version
import com.android.build.gradle.internal.pipeline.StreamBasedTask
import com.android.build.gradle.internal.tasks.CheckManifest
import com.android.build.gradle.internal.tasks.IncrementalTask
import com.android.build.gradle.tasks.ExtractAnnotations
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@CompileStatic
class AndroidCacheFixPlugin implements Plugin<Project> {
    private static List<Workaround> WORKAROUNDS = [
        new JavaCompile_BootClasspath_Workaround(),
        new JavaCompile_AnnotationProcessorSource_Workaround(),
        new AndroidJavaCompile_ProcessorListFile_Workaround(),
        new ExtractAnnotations_Source_Workaround(),
        new IncrementalTask_CombinedInput_Workaround(),
        new StreamBasedTask_CombinedInput_Workaround(),
        new ProcessAndroidResources_MergeBlameLogFolder_Workaround(),
        new CheckManifest_Manifest_Workaround(),
    ] as List<Workaround>

    @Override
	void apply(Project project) {
        project.afterEvaluate {
            def currentGradleVersion = GradleVersion.current()
            def currentAndroidVersion = VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)

            for (def workaround : WORKAROUNDS) {
                def fixedInGradleAnnotation = workaround.class.getAnnotation(FixedInGradle)
                if (fixedInGradleAnnotation != null
                    && currentGradleVersion >= GradleVersion.version(fixedInGradleAnnotation.value())) {
                    continue
                }
                def fixedInAndroidAnnotation = workaround.class.getAnnotation(FixedInAndroid)
                if (fixedInAndroidAnnotation != null
                    && currentAndroidVersion >= VersionNumber.parse(fixedInAndroidAnnotation.value())) {
                    continue
                }
                workaround.apply(project)
            }
        }
	}

	/**
	 * Fix {@link org.gradle.api.tasks.compile.CompileOptions#getBootClasspath()} introducing relocatability problems for {@link JavaCompile} and {@link AndroidJavaCompile}.
	 */
    @FixedInGradle("4.3")
	static class JavaCompile_BootClasspath_Workaround implements Workaround {
		@Override
		void apply(Project project) {
			project.tasks.withType(JavaCompile) { JavaCompile task ->
				task.inputs.property "options.bootClasspath", null
				task.inputs.files({ task.options.bootClasspath?.split(File.pathSeparator) })
						.withPathSensitivity(PathSensitivity.RELATIVE)
						.withPropertyName("options.bootClasspath.workaround")
			}
		}
	}

    /**
     * Filter the Java annotation processor output folder from compiler arguments to avoid absolute path.
     */
    @FixedInGradle("4.3")
    static class JavaCompile_AnnotationProcessorSource_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(JavaCompile) { JavaCompile task ->
                task.inputs.property "options.compilerArgs", null
                task.inputs.property "options.compilerArgs.workaround", {
                    def filteredArgs = []
                    def iCompilerArgs = task.options.compilerArgs.iterator()
                    while (iCompilerArgs.hasNext()) {
                        def compilerArg = iCompilerArgs.next()
                        if (compilerArg == "-s") {
                            if (iCompilerArgs.hasNext()) {
                                iCompilerArgs.next()
                            }
                        } else {
                            filteredArgs += compilerArg
                        }
                    }
                    return filteredArgs
                }

                // AndroidJavaCompile already declares this as annotationProcessorOutputFolder
                if (!(task instanceof AndroidJavaCompile)) {
                    task.outputs.dir {
                        def outputDir = null
                        def iCompilerArgs = task.options.compilerArgs.iterator()
                        while (iCompilerArgs.hasNext()) {
                            def compilerArg = iCompilerArgs.next()
                            if (compilerArg == "-s") {
                                if (iCompilerArgs.hasNext()) {
                                    outputDir = iCompilerArgs.next()
                                }
                            }
                        }
                        return outputDir
                    } withPropertyName("options.compilerArgs.annotationProcessorOutputFolder.workaround") optional()
                }
            }
        }
    }

    /**
     * Override path sensitivity for {@link AndroidJavaCompile#getProcessorListFile()} to {@link PathSensitivity#RELATIVE}.
     */
    @FixedInAndroid("3.1")
    static class AndroidJavaCompile_ProcessorListFile_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
                def originalValue = task.processorListFile
                setProcessorListFile(task, project.files())
                task.inputs.files(originalValue)
                    .withPathSensitivity(PathSensitivity.NONE)
                    .withPropertyName("processorListFile.workaround")

                task.doFirst {
                    setProcessorListFile(task, originalValue)
                }
            }
        }

        @CompileStatic(TypeCheckingMode.SKIP)
        private static void setProcessorListFile(AndroidJavaCompile task, FileCollection value) {
            task.processorListFile = value
        }
    }

    /**
     * Override path sensitivity for {@link ExtractAnnotations#getSource()} to {@link PathSensitivity#RELATIVE}.
     */
    @FixedInAndroid("3.1")
    static class ExtractAnnotations_Source_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(ExtractAnnotations) { ExtractAnnotations task ->
                def originalValue = task.source
                task.source = []
                task.inputs.files(originalValue)
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .withPropertyName("source.workaround")

                task.doFirst {
                    task.source = originalValue
                }
            }
        }
    }

    /**
     * Fix {@link IncrementalTask#getCombinedInput()} relocatability.
     */
    @FixedInAndroid("3.0.1")
    static class IncrementalTask_CombinedInput_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(IncrementalTask) { IncrementalTask task ->
                task.inputs.property "combinedInput", ""
                task.inputs.property "combinedInput.workaround", {
                    AndroidCacheFixPlugin.fixCombinedInputs(task.combinedInput)
                }
            }
        }

    }

    /**
     * Fix {@link StreamBasedTask#getCombinedInput()} relocatability.
     */
    @FixedInAndroid("3.0.1")
    static class StreamBasedTask_CombinedInput_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(StreamBasedTask) { StreamBasedTask task ->
                task.inputs.property "combinedInput", ""
                task.inputs.property "combinedInput.workaround", {
                    AndroidCacheFixPlugin.fixCombinedInputs(task.combinedInput)
                }
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static Map<String, Boolean> fixCombinedInputs(String combinedInputs) {
        combinedInputs.split("\n").collectEntries {
            def (propertyName, value) = it.split("=", 2)
            [(propertyName): (value != "null")]
        }
    }

    /**
     * {@link ProcessAndroidResources#getMergeBlameLogFolder()} shouldn't be an {@literal @}{@link org.gradle.api.tasks.Input}.
     */
    @FixedInAndroid("3.1")
    static class ProcessAndroidResources_MergeBlameLogFolder_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(ProcessAndroidResources) { ProcessAndroidResources task ->
                task.inputs.property "mergeBlameLogFolder", ""
            }
        }
    }

    /**
     * {@link com.android.build.gradle.internal.tasks.CheckManifest#getManifest()} should not be an {@literal @}{@link org.gradle.api.tasks.Input}.
     */
    @FixedInAndroid("3.0.1")
    static class CheckManifest_Manifest_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(CheckManifest) { CheckManifest task ->
                task.inputs.property "manifest", ""
            }
        }
    }
}
