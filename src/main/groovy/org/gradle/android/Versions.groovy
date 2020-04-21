package org.gradle.android

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final VersionNumber PLUGIN_VERSION;
    static final Set<GradleVersion> SUPPORTED_GRADLE_VERSIONS
    static final Set<VersionNumber> SUPPORTED_ANDROID_VERSIONS
    static final Multimap<VersionNumber, GradleVersion> SUPPORTED_VERSIONS_MATRIX

    static {
        def versions = new JsonSlurper().parse(AndroidCacheFixPlugin.classLoader.getResource("versions.json"))
        PLUGIN_VERSION = VersionNumber.parse(versions.version)

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        versions.supportedVersions.each { androidVersion, gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
        def matrix = builder.build()

        SUPPORTED_VERSIONS_MATRIX = matrix
        SUPPORTED_ANDROID_VERSIONS = ImmutableSortedSet.copyOf(matrix.keySet())
        SUPPORTED_GRADLE_VERSIONS = ImmutableSortedSet.copyOf(matrix.values())
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber latestAndroidVersion() {
        return SUPPORTED_ANDROID_VERSIONS.max()
    }

    static GradleVersion latestGradleVersion() {
        return SUPPORTED_GRADLE_VERSIONS.max()
    }

    static VersionNumber getLatestVersionForAndroid(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        return SUPPORTED_ANDROID_VERSIONS.findAll { it.major == versionNumber.major && it.minor == versionNumber.minor }.max()
    }
}
