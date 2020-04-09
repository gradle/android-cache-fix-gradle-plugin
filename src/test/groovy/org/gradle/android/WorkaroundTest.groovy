package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class WorkaroundTest extends Specification {

    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(Versions.android(androidVersion))
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/_Workaround/, "") }.sort() == expectedWorkarounds.sort()
        where:
        androidVersion  | expectedWorkarounds
        "3.6.2"         | []
        "3.6.1"         | []
        "3.6.0"         | []
        "3.5.3"         | []
        "3.5.2"         | []
        "3.5.1"         | []
        "3.5.0"         | []
    }
}
