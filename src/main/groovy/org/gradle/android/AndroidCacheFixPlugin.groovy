package org.gradle.android

import com.android.build.gradle.internal.pipeline.StreamBasedTask
import com.android.build.gradle.internal.tasks.IncrementalTask
import com.android.build.gradle.tasks.ExtractAnnotations
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.builder.model.Version
import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround_4_0
import org.gradle.android.workarounds.CompilerArgsProcessor
import org.gradle.android.workarounds.DexFileDependenciesWorkaround
import org.gradle.android.workarounds.MergeJavaResourcesWorkaround
import org.gradle.android.workarounds.MergeNativeLibsWorkaround
import org.gradle.android.workarounds.MergeResourcesWorkaround
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround_4_2
import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.android.workarounds.Workaround
import org.gradle.android.workarounds.WorkaroundContext
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.PathSensitivity
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method

import static org.gradle.android.Versions.SUPPORTED_ANDROID_VERSIONS
import static org.gradle.android.Versions.android
import static org.gradle.android.Versions.gradle

@CompileStatic
class AndroidCacheFixPlugin implements Plugin<Project> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidCacheFixPlugin)

    private static final String IGNORE_VERSION_CHECK_PROPERTY = "org.gradle.android.cache-fix.ignoreVersionCheck"
    private static final VersionNumber CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)

    private final List<Workaround> workarounds = [] as List<Workaround>

    private static boolean isSupportedAndroidVersion(Project project) {
        return systemPropertyBooleanCompat(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            SUPPORTED_ANDROID_VERSIONS.contains(CURRENT_ANDROID_VERSION)
    }

    private static boolean isMaybeSupportedAndroidVersion(Project project) {
        return systemPropertyBooleanCompat(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            (CURRENT_ANDROID_VERSION <= Versions.latestAndroidVersion() &&
                CURRENT_ANDROID_VERSION >= Versions.earliestMaybeSupportedAndroidVersion())
    }

    /**
     * Backward-compatible boolean system property check. This allows use of new ProviderFactory methods
     * on newer Gradle versions while falling back to old APIs gracefully on older APIs.
     *
     * @param key the key to look up.
     * @param project the source gradle project.
     * @return the system property value or false if absent.
     */
    private static boolean systemPropertyBooleanCompat(String key, Project project) {
        if (gradle(project.gradle.gradleVersion) >= GradleVersion.version("6.1")) {
            return project.providers.systemProperty(key)
                .forUseAtConfigurationTime()
                .map {
                    try {
                        Boolean.parseBoolean(it)
                    } catch (IllegalArgumentException | NullPointerException ignored) {
                        false
                    }
                }
                .getOrElse(false)
        } else {
            return Boolean.getBoolean(key)
        }
    }

    @Override
    void apply(Project project) {
        // This avoids trying to apply these workarounds to a build with a version of Android that does not contain
        // some of the classes the workarounds reference.  In such a case, we can throw a friendlier "not supported"
        // error instead of a ClassDefNotFound.
        if (isMaybeSupportedAndroidVersion(project)) {
            workarounds.addAll(
                new MergeJavaResourcesWorkaround(),
                new MergeNativeLibsWorkaround(),
                new RoomSchemaLocationWorkaround(),
                new CompileLibraryResourcesWorkaround_4_0(),
                new CompileLibraryResourcesWorkaround_4_2(),
                new MergeResourcesWorkaround(),
                new DexFileDependenciesWorkaround(),
            )
        }

        if (!isSupportedAndroidVersion(project)) {
            if (isMaybeSupportedAndroidVersion(project)) {
                project.logger.warn("WARNING: Android plugin ${CURRENT_ANDROID_VERSION} has not been tested with this version of the Android cache fix plugin, although it may work.  We test against only the latest patch release versions of Android Gradle plugin: ${SUPPORTED_ANDROID_VERSIONS.join(", ")}.  If ${CURRENT_ANDROID_VERSION} is newly released, we may not have had a chance to release a version tested against it yet.  Proceed with caution.  You can suppress this warning with with -D${IGNORE_VERSION_CHECK_PROPERTY}=true.")
            } else {
                throw new RuntimeException("Android plugin ${CURRENT_ANDROID_VERSION} is not supported by Android cache fix plugin. Supported Android plugin versions: ${SUPPORTED_ANDROID_VERSIONS.join(", ")}. Override with -D${IGNORE_VERSION_CHECK_PROPERTY}=true.")
            }
        }

        def context = new WorkaroundContext(project, new CompilerArgsProcessor(project))

        def appliedWorkarounds = []
        getWorkaroundsToApply(CURRENT_ANDROID_VERSION, project, workarounds).each { Workaround workaround ->
            LOGGER.debug("Applying Android workaround {} to {}", workaround.getClass().simpleName, project)
            workaround.apply(context)
            appliedWorkarounds += workaround.getClass().simpleName - "Workaround"
        }

        // We do this rather than trigger off of the plugin application because in Gradle 6.x the plugin is
        // applied to the Settings object which we don't have access to at this point
        project.afterEvaluate {
            def extension = project.rootProject.getExtensions().findByName("buildScan")
            if (extension) {
                Method valueMethod = extension.class.getMethod("value", String.class, String.class)
                if (valueMethod) {
                    valueMethod.invoke(extension, "${project.path} applied workarounds".toString(), appliedWorkarounds.join("\n"))
                    LOGGER.debug("Added build scan custom value for ${project.path} applied workarounds")
                }
            }
        }
    }

    static List<Workaround> getWorkaroundsToApply(
        VersionNumber androidVersion,
        Project project,
        List<Workaround> workarounds
    ) {
        def workaroundsBuilder = ImmutableList.<Workaround>builder()
        for (def workaround : workarounds) {
            def androidIssue = workaround.class.getAnnotation(AndroidIssue)
            def introducedIn = android(androidIssue.introducedIn())
            if (androidVersion < introducedIn) {
                continue
            }

            if (androidIssue.fixedIn().any { String fixedInVersionString ->
                def fixedInVersion = android(fixedInVersionString)
                androidVersion.baseVersion == fixedInVersion.baseVersion || androidVersion >= fixedInVersion
            }) {
                continue
            }

            if (project != null) {
                if (!workaround.canBeApplied(project)) {
                    continue
                }
            }

            workaroundsBuilder.add(workaround)
        }
        workaroundsBuilder.build()
    }

    /**
     * The following are kept for reference purposes.
     */

    /**
     * Fix {@link org.gradle.api.tasks.compile.CompileOptions#getBootClasspath()} introducing relocatability problems for {@link AndroidJavaCompile}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.1.0-alpha06", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68392933")
    static class AndroidJavaCompile_BootClasspath_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
                task.inputs.property "options.bootClasspath", ""
                // Override workaround introduced in 3.1.0-alpha02
                task.inputs.property "options.bootClasspath.filtered", ""
                task.inputs.files({
                        DeprecationLogger.whileDisabled({
                            //noinspection GrDeprecatedAPIUsage
                            task.options.bootClasspath?.split(File.pathSeparator)
                        } as Factory)
                    })
                    .withPathSensitivity(PathSensitivity.RELATIVE)
                    .withPropertyName("options.bootClasspath.workaround")
                    .optional(true)
            }
        }
    }
     */

    /**
     * Filter the Java annotation processor output folder from compiler arguments to avoid absolute path.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.1.0-alpha06", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68391973")
    static class AndroidJavaCompile_AnnotationProcessorSource_Workaround implements Workaround {
        @Override
        void apply(WorkaroundContext context) {
            context.compilerArgsProcessor.addRule(SkipNext.matching("-s"))
        }
    }
     */

    /**
     * Override path sensitivity for {@link AndroidJavaCompile#getProcessorListFile()} to {@link PathSensitivity#NONE}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.1.0-alpha01", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68759178")
    static class AndroidJavaCompile_ProcessorListFile_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
                def originalValue

                project.gradle.taskGraph.beforeTask {
                    if (task == it) {
                        originalValue = task.processorListFile
                        task.processorListFile = project.files()
                        task.inputs.files(originalValue)
                            .withPathSensitivity(PathSensitivity.NONE)
                            .withPropertyName("processorListFile.workaround")
                    }
                }

                task.doFirst {
                    task.processorListFile = originalValue
                }
            }
        }
    }
     */

    /**
     * Override path sensitivity for {@link AndroidJavaCompile#getDataBindingDependencyArtifacts()} to {@link PathSensitivity#RELATIVE}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = "3.2.0-alpha18", link = "https://issuetracker.google.com/issues/69243050")
    static class DataBindingDependencyArtifacts_Workaround implements Workaround {
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            def compilerArgsProcessor = context.compilerArgsProcessor
            compilerArgsProcessor.addRule(Skip.matching("-Aandroid.databinding.classLogFile=.*"))
            compilerArgsProcessor.addRule(Skip.matching("-Aandroid.databinding.sdkDir=.*"))
            compilerArgsProcessor.addRule(Skip.matching("-Aandroid.databinding.bindingBuildFolder=.*"))
            compilerArgsProcessor.addRule(Skip.matching("-Aandroid.databinding.xmlOutDir=.*"))
            compilerArgsProcessor.addRule(InputDirectory.withAnnotationProcessorArgument("android.databinding.baseFeatureInfo"))

            def outputRules = [
                AnnotationProcessorOverride.of("android.databinding.generationalFileOutDir") { Task task, String path ->
                    task.outputs.dir(path)
                        .withPropertyName("android.databinding.generationalFileOutDir.workaround")
                },
                AnnotationProcessorOverride.of("android.databinding.exportClassListTo") { Task task, String path ->
                    task.outputs.file(path)
                        .withPropertyName("android.databinding.exportClassListTo")
                }
            ]
            outputRules.each {
                compilerArgsProcessor.addRule it
            }

            project.tasks.withType(AndroidJavaCompile) { AndroidJavaCompile task ->
                reconfigurePathSensitivityForDataBindingDependencyArtifacts(project, task)
                filterDataBindingInfoFromSource(project, task)
                configureAdditionalOutputs(project, task, outputRules)
            }
        }

        @CompileStatic(TypeCheckingMode.SKIP)
        private
        static void reconfigurePathSensitivityForDataBindingDependencyArtifacts(Project project, AndroidJavaCompile task) {
            def originalValue

            project.gradle.taskGraph.beforeTask {
                if (task == it) {
                    originalValue = task.dataBindingDependencyArtifacts
                    if (originalValue != null) {
                        task.dataBindingDependencyArtifacts = project.files()
                        task.inputs.files(originalValue)
                            .withPathSensitivity(PathSensitivity.RELATIVE)
                            .withPropertyName("dataBindingDependencyArtifacts.workaround")
                    }
                }
            }

            task.doFirst {
                task.dataBindingDependencyArtifacts = originalValue
            }
        }

        @CompileStatic(TypeCheckingMode.SKIP)
        private static void filterDataBindingInfoFromSource(Project project, AndroidJavaCompile task) {
            def originalValue

            project.gradle.taskGraph.beforeTask {
                if (task == it) {
                    originalValue = task.source
                    if (originalValue != null) {
                        task.source = project.files()
                        def filteredSources = originalValue.matching { PatternFilterable filter ->
                            filter.exclude("android/databinding/layouts/DataBindingInfo.java")
                        }
                        task.inputs.files(filteredSources)
                            .withPathSensitivity(PathSensitivity.RELATIVE)
                            .withPropertyName("source.workaround")
                            .skipWhenEmpty()
                    }
                }
            }

            task.doFirst {
                task.source = originalValue
            }
        }


        @CompileStatic(TypeCheckingMode.SKIP)
        private static void configureAdditionalOutputs(Project project, AndroidJavaCompile task, List<AnnotationProcessorOverride> overrides) {
            def configTask = project.tasks.create("configure" + task.name.capitalize()) { configTask ->
                configTask.doFirst {
                    overrides.each {
                        it.configureAndroidJavaCompile(task)
                    }
                }
            }
            task.dependsOn configTask
        }
    }
     */

    /**
     * Override path sensitivity for {@link ExtractAnnotations#getSource()} to {@link PathSensitivity#RELATIVE}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.1.0-alpha01", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68759476")
    static class ExtractAnnotations_Source_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(ExtractAnnotations) { ExtractAnnotations task ->
                def originalValue

                project.gradle.taskGraph.beforeTask {
                    if (task == it) {
                        originalValue = task.source
                        task.source = []
                        task.inputs.files(originalValue)
                            .withPathSensitivity(PathSensitivity.RELATIVE)
                            .withPropertyName("source.workaround")
                            .skipWhenEmpty(true)
                    }
                }

                task.doFirst {
                    task.source = originalValue
                }
            }
        }
    }
     */

    /**
     * Fix {@link IncrementalTask#getCombinedInput()} and {@link StreamBasedTask#getCombinedInput()} relocatability.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.0.1", "3.1.0-alpha04", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68771542")
    static class CombinedInput_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(IncrementalTask) { IncrementalTask task ->
                task.inputs.property "combinedInput", ""
                task.inputs.property "combinedInput.workaround", {
                    fixCombinedInputs(task.combinedInput)
                }
            }
            project.tasks.withType(StreamBasedTask) { StreamBasedTask task ->
                task.inputs.property "combinedInput", ""
                task.inputs.property "combinedInput.workaround", {
                    fixCombinedInputs(task.combinedInput)
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
    }
     */

    /**
     * {@link ProcessAndroidResources#getMergeBlameLogFolder()} shouldn't be an {@literal @}{@link org.gradle.api.tasks.Input}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.0.1", "3.1.0-alpha02", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68385486")
    static class ProcessAndroidResources_MergeBlameLogFolder_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(ProcessAndroidResources) { ProcessAndroidResources task ->
                task.inputs.property "mergeBlameLogFolder", ""
            }
        }
    }
     */

    /**
     * {@link com.android.build.gradle.internal.tasks.CheckManifest#getManifest()} should not be an {@literal @}{@link org.gradle.api.tasks.Input}.

    @AndroidIssue(introducedIn = "3.0.0", fixedIn = ["3.1.0-alpha05", "3.1.1", "3.1.2", "3.1.3", "3.1.4", "3.2.0-alpha01"], link = "https://issuetracker.google.com/issues/68772035")
    static class CheckManifest_Manifest_Workaround implements Workaround {
        @CompileStatic(TypeCheckingMode.SKIP)
        @Override
        void apply(WorkaroundContext context) {
            def project = context.project
            project.tasks.withType(CheckManifest) { CheckManifest task ->
                task.inputs.property "manifest", ""
            }
        }
    }
     */
}
