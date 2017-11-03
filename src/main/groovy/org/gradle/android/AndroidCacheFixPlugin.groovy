package org.gradle.android

import com.android.build.gradle.internal.Version
import com.android.build.gradle.internal.pipeline.StreamBasedTask
import com.android.build.gradle.internal.tasks.CheckManifest
import com.android.build.gradle.internal.tasks.IncrementalTask
import com.android.build.gradle.tasks.ExtractAnnotations
import com.android.build.gradle.tasks.PackageApplication
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.build.gradle.tasks.factory.AndroidJavaCompile
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.PathSensitivity
import org.gradle.internal.Factory
import org.gradle.util.DeprecationLogger
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
class AndroidCacheFixPlugin implements Plugin<Project> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidCacheFixPlugin)

    private static List<Workaround> WORKAROUNDS = [
        new AndroidJavaCompile_BootClasspath_Workaround(),
        new AndroidJavaCompile_AnnotationProcessorSource_Workaround(),
        new AndroidJavaCompile_ProcessorListFile_Workaround(),
        new ExtractAnnotations_Source_Workaround(),
        new IncrementalTask_CombinedInput_Workaround(),
        new StreamBasedTask_CombinedInput_Workaround(),
        new ProcessAndroidResources_MergeBlameLogFolder_Workaround(),
        new CheckManifest_Manifest_Workaround(),
        new PackageApplication_Disable_Workaround(),
    ] as List<Workaround>


    @Override
	void apply(Project project) {
        project.afterEvaluate {
            def currentGradleVersion = GradleVersion.current().baseVersion
            def currentAndroidVersion = VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION).baseVersion

            if (!SupportedVersions.ANDROID_VERSIONS.contains(currentAndroidVersion)) {
                DeprecationLogger.nagUserWith("Android plugin $currentGradleVersion is not supported by Android cache fix plugin, not applying workarounds")
                return
            }
            if (!SupportedVersions.GRADLE_VERSIONS.contains(currentGradleVersion)) {
                DeprecationLogger.nagUserWith("$currentGradleVersion is not supported by Android cache fix plugin, not applying workarounds")
                return
            }

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
                LOGGER.debug("Applying Android workaround {} to {}", workaround.getClass().simpleName, project)
                workaround.apply(project)
            }
        }
	}

	/**
	 * Fix {@link org.gradle.api.tasks.compile.CompileOptions#getBootClasspath()} introducing relocatability problems for {@link AndroidJavaCompile}.
	 */
	static class AndroidJavaCompile_BootClasspath_Workaround implements Workaround {
		@Override
		void apply(Project project) {
			project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
				task.inputs.property "options.bootClasspath", (String) null
				task.inputs.files({
                        DeprecationLogger.whileDisabled({
                            task.options.bootClasspath?.split(File.pathSeparator)
                        } as Factory)
                    })
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .withPropertyName("options.bootClasspath.workaround")
			}
		}
	}

    /**
     * Filter the Java annotation processor output folder from compiler arguments to avoid absolute path.
     */
    static class AndroidJavaCompile_AnnotationProcessorSource_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
                task.inputs.property "options.compilerArgs", (String) null
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
    static class IncrementalTask_CombinedInput_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(IncrementalTask) { IncrementalTask task ->
                task.inputs.property "combinedInput", (String) null
                task.inputs.property "combinedInput.workaround", {
                    AndroidCacheFixPlugin.fixCombinedInputs(task.combinedInput)
                }
            }
        }

    }

    /**
     * Fix {@link StreamBasedTask#getCombinedInput()} relocatability.
     */
    static class StreamBasedTask_CombinedInput_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(StreamBasedTask) { StreamBasedTask task ->
                task.inputs.property "combinedInput", (String) null
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
    static class ProcessAndroidResources_MergeBlameLogFolder_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(ProcessAndroidResources) { ProcessAndroidResources task ->
                task.inputs.property "mergeBlameLogFolder", (String) null
            }
        }
    }

    /**
     * {@link com.android.build.gradle.internal.tasks.CheckManifest#getManifest()} should not be an {@literal @}{@link org.gradle.api.tasks.Input}.
     */
    static class CheckManifest_Manifest_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(CheckManifest) { CheckManifest task ->
                task.inputs.property "manifest", (String) null
            }
        }
    }

    /**
     * Caching {@link PackageApplication} is not efficient with the current way caching works in Gradle.
     */
    static class PackageApplication_Disable_Workaround implements Workaround {
        @Override
        void apply(Project project) {
            project.tasks.withType(PackageApplication) { PackageApplication task ->
                task.outputs.doNotCacheIf("PackageApplication should not be cacheable") {
                    PackageApplication.class.getAnnotation(CacheableTask) != null
                }
            }
        }
    }
}
