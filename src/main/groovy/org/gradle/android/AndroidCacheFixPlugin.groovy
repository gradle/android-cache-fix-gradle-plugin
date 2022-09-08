package org.gradle.android

import com.google.common.collect.ImmutableList
import groovy.transform.CompileStatic

import org.gradle.android.workarounds.BundleLibraryClassesWorkaround

import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround
import org.gradle.android.workarounds.DataBindingMergeDependencyArtifactsWorkaround
import org.gradle.android.workarounds.JdkImageWorkaround
import org.gradle.android.workarounds.LibraryJniLibsWorkaround
import org.gradle.android.workarounds.MergeNativeLibsWorkaround

import org.gradle.android.workarounds.MergeSourceSetFoldersWorkaround
import org.gradle.android.workarounds.StripDebugSymbolsWorkaround
import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.android.workarounds.Workaround
import org.gradle.android.workarounds.ZipMergingTaskWorkaround
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.gradle.android.Versions.*

@CompileStatic
class AndroidCacheFixPlugin implements Plugin<Project> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AndroidCacheFixPlugin)

    private final List<Workaround> workarounds = [] as List<Workaround>

    static List<Workaround> initializeWorkarounds(Project project) {
        // This avoids trying to apply these workarounds to a build with a version of Android that does not contain
        // some of the classes the workarounds reference. In such a case, we can throw a friendlier "not supported"
        // error instead of a ClassDefNotFound.
        if (isMaybeSupportedAndroidVersion(project)) {
            return Arrays.<Workaround>asList(
                new MergeNativeLibsWorkaround(),
                new MergeSourceSetFoldersWorkaround(),
                new RoomSchemaLocationWorkaround(),
                new CompileLibraryResourcesWorkaround(),
                new StripDebugSymbolsWorkaround(),
                new BundleLibraryClassesWorkaround(),
                new DataBindingMergeDependencyArtifactsWorkaround(),
                new LibraryJniLibsWorkaround(),
                new ZipMergingTaskWorkaround(),
                new JdkImageWorkaround()
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
                Warnings.MAYBE_SUPPORTED_ANDROID_VERSION.warnOnce(project)
            } else {
                throw new RuntimeException("Android plugin ${CURRENT_ANDROID_VERSION} is not supported by Android cache fix plugin. Supported Android plugin versions: ${SUPPORTED_ANDROID_VERSIONS.join(", ")}. Please check if a newer version of this plugin is available or override with -D${IGNORE_VERSION_CHECK_PROPERTY}=true.")
            }
        }

        def appliedWorkarounds = []
        getWorkaroundsToApply(CURRENT_ANDROID_VERSION, project, workarounds).each { Workaround workaround ->
            LOGGER.debug("Applying Android workaround {} to {}", workaround.getClass().simpleName, project)
            workaround.apply(project)
            appliedWorkarounds += workaround.getClass().simpleName - "Workaround"
        }

        project.gradle.sharedServices.registerIfAbsent("warnings", WarningsService.class) {}.get()
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
}
