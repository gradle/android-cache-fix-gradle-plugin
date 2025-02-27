package org.gradle.android

import com.android.Version
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final Set<GradleVersion> TESTED_GRADLE_VERSIONS
    static final Set<VersionNumber> TESTED_ANDROID_VERSIONS
    static final Multimap<VersionNumber, GradleVersion> TESTED_VERSIONS_MATRIX
    static final VersionNumber CURRENT_ANDROID_VERSION

    static {
        def versions = new JsonSlurper().parse(AndroidCacheFixPlugin.classLoader.getResource("versions.json"))

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        versions.testedVersions.each { String androidVersion, List<String> gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
        def matrix = builder.build()

        TESTED_VERSIONS_MATRIX = matrix
        TESTED_ANDROID_VERSIONS = ImmutableSortedSet.copyOf(matrix.keySet())
        TESTED_GRADLE_VERSIONS = ImmutableSortedSet.copyOf(matrix.values())

        CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber earliestSupportedAndroidVersion() {
        VersionNumber earliestSupported = TESTED_ANDROID_VERSIONS.min()
        // "alpha" is lower than null
        return new VersionNumber(earliestSupported.major, earliestSupported.minor, 0, "alpha")
    }

    static boolean isSupportedAndroidVersion() {
        return CURRENT_ANDROID_VERSION >= earliestSupportedAndroidVersion()
    }
}
