package org.gradle.android

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.gradle.util.GradleVersion

class TestVersions {
    static Multimap<VersionNumber, GradleVersion> getAllCandidateTestVersions() {
        def testedVersion = System.getProperty('org.gradle.android.testVersion')
        if (testedVersion) {
            return ImmutableMultimap.copyOf(Versions.SUPPORTED_VERSIONS_MATRIX.entries().findAll {it.key == VersionNumber.parse(testedVersion) })
        } else {
            return Versions.SUPPORTED_VERSIONS_MATRIX
        }
    }

    static VersionNumber latestAndroidVersionForCurrentJDK() {
        return allCandidateTestVersions.keySet().max()
    }

    static GradleVersion latestGradleVersion() {
        return allCandidateTestVersions.values().max()
    }

    static GradleVersion latestSupportedGradleVersionFor(String androidVersion) {
        return latestSupportedGradleVersionFor(VersionNumber.parse(androidVersion))
    }

    static GradleVersion latestSupportedGradleVersionFor(VersionNumber androidVersion) {
        return allCandidateTestVersions.asMap().find {it.key.major == androidVersion.major && it.key.minor == androidVersion.minor }?.value?.max()
    }

    static VersionNumber getLatestVersionForAndroid(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        return allCandidateTestVersions.keySet().findAll { it.major == versionNumber.major && it.minor == versionNumber.minor }?.max()
    }

    static List<VersionNumber> getLatestAndroidVersions() {
        def minorVersions = allCandidateTestVersions.keySet().collect { "${it.major}.${it.minor}" }
        return minorVersions.collect { getLatestVersionForAndroid(it) }
    }

    // This map represents the Kotlin supported versions associated with Ksp supported versions
    static List<String> supportedKotlinVersions = ["1.7.22", "1.8.22", "1.9.0", "2.0.21"]

    static VersionNumber latestSupportedKotlinVersion() {
        return VersionNumber.parse(supportedKotlinVersions.last().toString())
    }

    static VersionNumber latestKotlinVersionForGradleVersion(GradleVersion gradleVersion) {
        return latestSupportedKotlinVersion()
    }
}
