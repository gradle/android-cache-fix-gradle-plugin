package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class WorkaroundTest extends Specification {

    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(Versions.android(androidVersion), null)
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/Workaround/, "") }.sort() == expectedWorkarounds.sort()
        where:
        androidVersion  | expectedWorkarounds
        "4.2.0-alpha05" | ['RoomSchemaLocation', 'CompileLibraryResources', 'MergeResources']
        "4.1.0-beta05"  | ['RoomSchemaLocation', 'CompileLibraryResources', 'MergeResources']
        "4.0.1"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation', 'CompileLibraryResources', 'MergeResources']
        "3.6.4"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation']
        "3.5.4"         | ['MergeJavaResources', 'RoomSchemaLocation']
    }
}
