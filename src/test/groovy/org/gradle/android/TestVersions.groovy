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

    static String kotlinVersion = "2.0.21"
    // AGP versions <= 7.0 can't use the kotlin-android plugin version 2.0
    static String kotlinVersionCompatibleWithOlderAgp = "1.9.0"

    static VersionNumber latestSupportedKotlinVersion() {
        // version 7.1.3 or higher should be used with kotlin-android plugin 2
        if(latestAndroidVersionForCurrentJDK() <= VersionNumber.parse("7.0.4")) {
           return VersionNumber.parse(kotlinVersionCompatibleWithOlderAgp)
        } else {
            return VersionNumber.parse(kotlinVersion)
        }
    }

    static VersionNumber latestKotlinVersionForGradleVersion(GradleVersion gradleVersion) {
        return latestSupportedKotlinVersion()
    }
}
