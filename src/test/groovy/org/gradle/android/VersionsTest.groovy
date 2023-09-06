package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class VersionsTest extends Specification {
    def "earliest supported version includes alpha and beta builds"() {
        def alphaOfEarliest = alphaVersionOf(Versions.SUPPORTED_ANDROID_VERSIONS.min())

        expect:
        alphaOfEarliest >= Versions.earliestSupportedAndroidVersion()
    }

    static VersionNumber alphaVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.0-alpha01")
    }
}
