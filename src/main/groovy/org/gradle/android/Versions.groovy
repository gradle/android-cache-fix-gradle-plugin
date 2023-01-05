package org.gradle.android

import com.android.Version
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.android.workarounds.SystemPropertiesCompat
import org.gradle.api.Project
import org.gradle.util.GradleVersion

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final Set<GradleVersion> SUPPORTED_GRADLE_VERSIONS
    static final Set<VersionNumber> SUPPORTED_ANDROID_VERSIONS
    static final Multimap<VersionNumber, GradleVersion> SUPPORTED_VERSIONS_MATRIX
    static final VersionNumber CURRENT_ANDROID_VERSION
    static final String IGNORE_VERSION_CHECK_PROPERTY = "org.gradle.android.cache-fix.ignoreVersionCheck"

    static {
        def versions = new JsonSlurper().parse(AndroidCacheFixPlugin.classLoader.getResource("versions.json"))
        def versions17 = new JsonSlurper().parse(AndroidCacheFixPlugin.classLoader.getResource("versions_17.json"))

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        parseSupportedVersions(versions, builder)
        parseSupportedVersions(versions17, builder)
        def matrix = builder.build()

        SUPPORTED_VERSIONS_MATRIX = matrix
        SUPPORTED_ANDROID_VERSIONS = ImmutableSortedSet.copyOf(matrix.keySet())
        SUPPORTED_GRADLE_VERSIONS = ImmutableSortedSet.copyOf(matrix.values())

        CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    }

    private static Object parseSupportedVersions(versions, builder) {
        versions.supportedVersions.each { String androidVersion, List<String> gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber earliestMaybeSupportedAndroidVersion() {
        VersionNumber earliestSupported = SUPPORTED_ANDROID_VERSIONS.min()
        // "alpha" is lower than null
        return new VersionNumber(earliestSupported.major, earliestSupported.minor, 0, "alpha")
    }

    static VersionNumber latestAndroidVersion() {
        return SUPPORTED_ANDROID_VERSIONS.max()
    }

    static boolean isSupportedAndroidVersion(Project project) {
        return SystemPropertiesCompat.getBoolean(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            SUPPORTED_ANDROID_VERSIONS.contains(CURRENT_ANDROID_VERSION)
    }

    static boolean isMaybeSupportedAndroidVersion(Project project) {
        return SystemPropertiesCompat.getBoolean(IGNORE_VERSION_CHECK_PROPERTY, project) ||
            isSameMajorAndMinorAsSupportedVersion()
    }

    static boolean isSameMajorAndMinorAsSupportedVersion() {
        return isSameMajorAndMinorAsSupportedVersion(CURRENT_ANDROID_VERSION)
    }

    static boolean isSameMajorAndMinorAsSupportedVersion(VersionNumber versionNumber) {
        return SUPPORTED_ANDROID_VERSIONS.stream().anyMatch { supportedVersion ->
            versionNumber.major == supportedVersion.major && versionNumber.minor == supportedVersion.minor
        }
    }
}
