package org.gradle.android

import com.android.Version
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.json.JsonParserType
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final Set<GradleVersion> SUPPORTED_GRADLE_VERSIONS
    static final Set<VersionNumber> SUPPORTED_ANDROID_VERSIONS
    static final Multimap<VersionNumber, GradleVersion> SUPPORTED_VERSIONS_MATRIX
    static final VersionNumber CURRENT_ANDROID_VERSION

    static {
        def slurper = new JsonSlurper().setType(JsonParserType.LAX)
        def versions = slurper.parse(AndroidCacheFixPlugin.classLoader.getResource("versions.json5"))

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        versions.supportedVersions.each { String androidVersion, List<String> gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
        def matrix = builder.build()

        SUPPORTED_VERSIONS_MATRIX = matrix
        SUPPORTED_ANDROID_VERSIONS = ImmutableSortedSet.copyOf(matrix.keySet())
        SUPPORTED_GRADLE_VERSIONS = ImmutableSortedSet.copyOf(matrix.values())

        CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber earliestSupportedAndroidVersion() {
        VersionNumber earliestSupported = SUPPORTED_ANDROID_VERSIONS.min()
        // "alpha" is lower than null
        return new VersionNumber(earliestSupported.major, earliestSupported.minor, 0, "alpha")
    }

    static boolean isSupportedAndroidVersion() {
        return CURRENT_ANDROID_VERSION >= earliestSupportedAndroidVersion()
    }
}
