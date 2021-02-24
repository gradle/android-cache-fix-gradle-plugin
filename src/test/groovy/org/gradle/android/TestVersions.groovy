package org.gradle.android

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber


class TestVersions {
    static final VersionNumber FIRST_JDK11_ANDROID_VERSION = VersionNumber.parse("7.0.0-alpha01")

    static Multimap<VersionNumber, GradleVersion> getAllCandidateVersions() {
        def testedVersion = System.getProperty('org.gradle.android.testVersion')
        if (testedVersion) {
            return ImmutableMultimap.copyOf(Versions.SUPPORTED_VERSIONS_MATRIX.entries().findAll {it.key == VersionNumber.parse(testedVersion) })
        } else {
            return Versions.SUPPORTED_VERSIONS_MATRIX
        }
    }

    static Multimap<VersionNumber, GradleVersion> getAllSupportedVersionsForCurrentJDK() {
        return ImmutableMultimap.copyOf(allCandidateVersions.entries().findAll {isAndroidVersionSupportedOnCurrentJDK(it.key) })
    }

    static boolean isAndroidVersionSupportedOnCurrentJDK(VersionNumber androidVersion) {
        return (JavaVersion.current().isJava8() && androidVersion < FIRST_JDK11_ANDROID_VERSION) ||
            (JavaVersion.current().isJava11() && androidVersion >= FIRST_JDK11_ANDROID_VERSION)
    }

    static VersionNumber latestAndroidVersionForCurrentJDK() {
        return allSupportedVersionsForCurrentJDK.keySet().max()
    }

    static GradleVersion latestGradleVersion() {
        return allSupportedVersionsForCurrentJDK.values().max()
    }

    static GradleVersion latestSupportedGradleVersionFor(String androidVersion) {
        return latestSupportedGradleVersionFor(VersionNumber.parse(androidVersion))
    }

    static GradleVersion latestSupportedGradleVersionFor(VersionNumber androidVersion) {
        return allSupportedVersionsForCurrentJDK.asMap().find {it.key.major == androidVersion.major && it.key.minor == androidVersion.minor }?.value?.max()
    }

    static VersionNumber getLatestVersionForAndroid(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        return allSupportedVersionsForCurrentJDK.keySet().findAll { it.major == versionNumber.major && it.minor == versionNumber.minor }?.max()
    }

    static List<VersionNumber> getLatestAndroidVersions() {
        def minorVersions = allSupportedVersionsForCurrentJDK.keySet().collect { "${it.major}.${it.minor}" }
        return minorVersions.collect { getLatestVersionForAndroid(it) }
    }
}
