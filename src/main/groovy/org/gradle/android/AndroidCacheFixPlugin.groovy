package org.gradle.android

import com.android.build.gradle.internal.Version
import com.android.build.gradle.internal.pipeline.StreamBasedTask
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
        new AndroidProcessResources_MergeBlameLogFolder_Workaround()
    ]

    @Override
	void apply(Project project) {
		def currentGradleVersion = GradleVersion.current()
		def currentAndroidVersion = VersionNumber.parse(Version.ANDROID_GRADLE_PLUGIN_VERSION)

		for (def workaround : WORKAROUNDS) {
			if (workaround.gradleFixVersion != null
				&& currentGradleVersion >= workaround.gradleFixVersion) {
				continue
			}
			if (workaround.androidFixVersion != null
				&& currentAndroidVersion >= workaround.androidFixVersion) {
				continue
			}
			workaround.apply(project)
		}
	}

	static abstract class Workaround {
        /**
         * Version of Gradle that fixes the problem. Workaround not applied if current Gradle version is the same or later.
         */
		GradleVersion getGradleFixVersion() {
			null
		}

        /**
         * Version of Android plugin that fixes the problem. Workaround not applied if current Android plugin version is the same or later.
         */
		VersionNumber getAndroidFixVersion() {
			null
		}

		abstract void apply(Project project)
	}

	/**
	 * Workaround to avoid absolute paths making it into the configuration of JavaCompile and AndroidJavaCompile tasks
	 */
	static class JavaCompile_BootClasspath_Workaround extends Workaround {
		@Override
		GradleVersion getGradleFixVersion() {
			GradleVersion.version("4.3")
		}

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
     * Filter the annotation processor output folder from compiler arguments to avoid absolute path.
     */
    static class JavaCompile_AnnotationProcessorSource_Workaround extends Workaround {
        @Override
        GradleVersion getGradleFixVersion() {
            GradleVersion.version("4.3")
        }

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
     * Override path sensitivity for AndroidJavaCompile.processorListFile.
     */
    static class AndroidJavaCompile_ProcessorListFile_Workaround extends Workaround {
        @Override
        VersionNumber getAndroidFixVersion() {
            return VersionNumber.parse("3.1")
        }

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
     * Override path sensitivity for ExtractAnnotations.source.
     */
    static class ExtractAnnotations_Source_Workaround extends Workaround {
        @Override
        VersionNumber getAndroidFixVersion() {
            return VersionNumber.parse("3.1")
        }

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
     * Fix IncrementalTask.combinedInputs.
     */
    static class IncrementalTask_CombinedInput_Workaround extends Workaround {
        @Override
        VersionNumber getAndroidFixVersion() {
            return VersionNumber.parse("3.0.1")
        }

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
     * Fix StreamBasedTask.combinedInputs.
     */
    static class StreamBasedTask_CombinedInput_Workaround extends Workaround {
        @Override
        VersionNumber getAndroidFixVersion() {
            return VersionNumber.parse("3.0.1")
        }

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
    static Map<String, Boolean> fixCombinedInputs(String combinedInputs) {
        combinedInputs.split("\n").collectEntries {
            def (propertyName, value) = it.split("=", 2)
            [(propertyName): (value != "null")]
        }
    }

    static class AndroidProcessResources_MergeBlameLogFolder_Workaround extends Workaround {
        @Override
        VersionNumber getAndroidFixVersion() {
            return VersionNumber.parse("3.1")
        }

        @Override
        void apply(Project project) {
            project.tasks.withType(ProcessAndroidResources) { ProcessAndroidResources task ->
                task.inputs.property "mergeBlameLogFolder", ""
            }
        }
    }
}
