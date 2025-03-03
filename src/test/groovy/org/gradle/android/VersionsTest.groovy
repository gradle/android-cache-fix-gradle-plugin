package org.gradle.android

import spock.lang.Specification

class VersionsTest extends Specification {
    def "earliest tested version corresponds to minimum supported version"() {
        def alphaOfEarliest = alphaVersionOf(TestVersions.TESTED_ANDROID_VERSIONS.min())

        expect:
        alphaOfEarliest == Versions.MINIMUM_ANDROID_VERSION
    }

    static VersionNumber alphaVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.0-alpha01")
    }
}
