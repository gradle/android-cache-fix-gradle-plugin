package org.gradle.android

import spock.lang.Specification

class VersionsTest extends Specification {
    def "earliest supported version includes alpha and beta builds"() {
        def alphaOfEarliest = alphaVersionOf(Versions.TESTED_ANDROID_VERSIONS.min())

        expect:
        alphaOfEarliest >= Versions.earliestSupportedAndroidVersion()
    }

    static VersionNumber alphaVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.0-alpha01")
    }
}
