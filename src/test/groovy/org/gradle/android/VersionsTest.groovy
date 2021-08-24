package org.gradle.android

import org.gradle.util.VersionNumber
import spock.lang.Specification
import spock.lang.Unroll

class VersionsTest extends Specification {
    def "earliest supported version includes alpha and beta builds"() {
        def alphaOfEarliest = alphaVersionOf(Versions.SUPPORTED_ANDROID_VERSIONS.min())

        expect:
        alphaOfEarliest >= Versions.earliestMaybeSupportedAndroidVersion()
    }

    @Unroll
    def "recognizes versions that match major/minor of supported versions (#androidVersion)"() {
        expect:
        Versions.isSameMajorAndMinorAsSupportedVersion(androidVersion)

        where:
        androidVersion << Versions.SUPPORTED_ANDROID_VERSIONS.collect {[
            newPatchVersionOf(it),
            alphaVersionOf(it),
            betaVersionOf(it)
        ]}.flatten()
    }

    @Unroll
    def "does not recognize versions that differ from major/minor of supported versions (#androidVersion)"() {
        expect:
        !Versions.isSameMajorAndMinorAsSupportedVersion(androidVersion)

        where:
        androidVersion << newMinorVersionForEachSupportedMajorVersion()
    }

    static VersionNumber newPatchVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.${versionNumber.micro + 1}")
    }

    static VersionNumber newMinorVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor + 1}.${versionNumber.micro}")
    }

    static VersionNumber alphaVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.0-alpha01")
    }

    static VersionNumber betaVersionOf(VersionNumber versionNumber) {
        return Versions.android("${versionNumber.major}.${versionNumber.minor}.0-beta01")
    }

    static newMinorVersionForEachSupportedMajorVersion() {
        return (Versions.SUPPORTED_ANDROID_VERSIONS.collect {it.major} as Set).collect {majorVersion ->
            Versions.SUPPORTED_ANDROID_VERSIONS.findAll { it.major == majorVersion }.max()
        }.collect {newMinorVersionOf(it) }
    }
}
