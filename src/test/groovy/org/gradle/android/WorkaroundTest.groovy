package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class WorkaroundTest extends Specification {
    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def possibleWorkarounds = AndroidCacheFixPlugin.initializeWorkarounds()
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(Versions.android(androidVersion), null, possibleWorkarounds)
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/Workaround/, "") }.sort() == expectedWorkarounds.sort()
        where:
        androidVersion | expectedWorkarounds
        "9.0"          | ['JdkImage']
        "8.13"         | ['JdkImage']
        "8.12"         | ['JdkImage']
        "8.11"         | ['JdkImage']
        "8.10"         | ['JdkImage']
        "8.9"          | ['JdkImage']
        "8.8"          | ['JdkImage']
        "8.7"          | ['JdkImage']
        "8.6"          | ['JdkImage']
        "8.5"          | ['JdkImage']
        "8.4"          | ['JdkImage']
    }
}
