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
        "4.2.0-alpha04" | ['RoomSchemaLocation', 'CompileLibraryResources']
        "4.1.0-beta04" | ['RoomSchemaLocation', 'CompileLibraryResources']
        "4.0.0"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation', 'CompileLibraryResources']
        "3.6.2"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation']
        "3.6.1"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation']
        "3.6.0"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation']
        "3.5.3"         | ['MergeJavaResources', 'RoomSchemaLocation']
        "3.5.2"         | ['MergeJavaResources', 'RoomSchemaLocation']
        "3.5.1"         | ['MergeJavaResources', 'RoomSchemaLocation']
        "3.5.0"         | ['MergeJavaResources', 'RoomSchemaLocation']
    }
}
