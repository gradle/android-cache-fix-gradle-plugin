package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class WorkaroundTest extends Specification {

    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(Versions.android(androidVersion))
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/Workaround/, "") }.sort() == expectedWorkarounds.sort()
        where:
        androidVersion  | expectedWorkarounds
        "4.0.0-beta04"  | ['MergeJavaResources', 'MergeNativeLibs']
        "3.6.2"         | ['MergeJavaResources', 'MergeNativeLibs']
        "3.6.1"         | ['MergeJavaResources', 'MergeNativeLibs']
        "3.6.0"         | ['MergeJavaResources', 'MergeNativeLibs']
        "3.5.3"         | ['MergeJavaResources']
        "3.5.2"         | ['MergeJavaResources']
        "3.5.1"         | ['MergeJavaResources']
        "3.5.0"         | ['MergeJavaResources']
    }
}
