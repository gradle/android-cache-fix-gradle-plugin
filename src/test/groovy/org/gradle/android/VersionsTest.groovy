package org.gradle.android

import org.junit.Assert
import org.junit.Test

class VersionsTest {

    @Test
    void "earliest supported version includes alpha and beta builds"() {
        def earliest = Versions.SUPPORTED_ANDROID_VERSIONS.min()
        def alphaOfEarliest = Versions.android("${earliest.major}.${earliest.minor}.0-alpha01")
        Assert.assertTrue(alphaOfEarliest >= Versions.earliestMaybeSupportedAndroidVersion())
    }
}
