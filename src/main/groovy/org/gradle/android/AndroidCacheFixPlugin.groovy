package org.gradle.android

import com.android.builder.model.Version
import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround_4_0
import org.gradle.android.workarounds.CompilerArgsProcessor
import org.gradle.android.workarounds.MergeJavaResourcesWorkaround
import org.gradle.android.workarounds.MergeNativeLibsWorkaround
import org.gradle.android.workarounds.MergeResourcesWorkaround
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround_4_2
import org.gradle.android.workarounds.StripDebugSymbolsWorkaround
import org.gradle.android.workarounds.SystemPropertiesCompat
import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.android.workarounds.Workaround
import org.gradle.android.workarounds.WorkaroundContext
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.VersionNumber
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean

import static org.gradle.android.Versions.SUPPORTED_ANDROID_VERSIONS
import static org.gradle.android.Versions.android

@CompileStatic
class AndroidCacheFixPlugin implements Plugin<Project> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidCacheFixPlugin)

    static final String IGNORE_VERSION_CHECK_PROPERTY = "org.gradle.android.cache-fix.ignoreVersionCheck"
    static final VersionNumber CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)

    private final List<Workaround> workarounds = [] as List<Workaround>

    private static boolean isSupportedAndroidVersion(Project project) {
        return SystemPropertiesCompat.getBoolean(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            SUPPORTED_ANDROID_VERSIONS.contains(CURRENT_ANDROID_VERSION)
    }

    private static boolean isMaybeSupportedAndroidVersion(Project project) {
        return SystemPropertiesCompat.getBoolean(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            (CURRENT_ANDROID_VERSION <= Versions.latestAndroidVersion() &&
                CURRENT_ANDROID_VERSION >= Versions.earliestMaybeSupportedAndroidVersion())
    }

    static List<Workaround> initializeWorkarounds(Project project) {
        // This avoids trying to apply these workarounds to a build with a version of Android that does not contain
        // some of the classes the workarounds reference. In such a case, we can throw a friendlier "not supported"
        // error instead of a ClassDefNotFound.
        if (isMaybeSupportedAndroidVersion(project)) {
            return Arrays.<Workaround>asList(
                new MergeJavaResourcesWorkaround(),
                new MergeNativeLibsWorkaround(),
                new RoomSchemaLocationWorkaround(),
                new CompileLibraryResourcesWorkaround_4_0(),
                new CompileLibraryResourcesWorkaround_4_2(),
                new MergeResourcesWorkaround(),
                new StripDebugSymbolsWorkaround()
            )
        } else {
            return Collections.emptyList()
        }
    }

    @Override
    void apply(Project project) {
        workarounds.addAll(initializeWorkarounds(project))

        if (!isSupportedAndroidVersion(project)) {
            if (isMaybeSupportedAndroidVersion(project)) {
                Warnings.MAYBE_SUPPORTED_ANDROID_VERSION.warnOnce(project.logger)
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

    private enum Warnings {
        MAYBE_SUPPORTED_ANDROID_VERSION("WARNING: Android plugin ${CURRENT_ANDROID_VERSION} has not been tested with this version of the Android cache fix plugin, although it may work.  We test against only the latest patch release versions of Android Gradle plugin: ${SUPPORTED_ANDROID_VERSIONS.join(", ")}.  If ${CURRENT_ANDROID_VERSION} is newly released, we may not have had a chance to release a version tested against it yet.  Proceed with caution.  You can suppress this warning with with -D${IGNORE_VERSION_CHECK_PROPERTY}=true.")

        private final String warning
        private final AtomicBoolean warned = new AtomicBoolean()

        Warnings(String warning) {
            this.warning = warning
        }

        void warnOnce(org.gradle.api.logging.Logger logger) {
            if (!warned.getAndSet(true)) {
                logger.warn(warning)
            }
        }
    }
}
