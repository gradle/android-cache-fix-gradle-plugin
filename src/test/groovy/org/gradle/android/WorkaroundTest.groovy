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
        "4.2.0-alpha15" | ['RoomSchemaLocation', 'CompileLibraryResources_4_2', 'MergeResources', 'DexFileDependencies']
        "4.1.0"  | ['RoomSchemaLocation', 'CompileLibraryResources_4_0', 'MergeResources', 'DexFileDependencies']
        "4.0.2"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation', 'CompileLibraryResources_4_0', 'MergeResources', 'DexFileDependencies']
        "3.6.4"         | ['MergeJavaResources', 'MergeNativeLibs', 'RoomSchemaLocation', 'DexFileDependencies']
        "3.5.4"         | ['MergeJavaResources', 'RoomSchemaLocation', 'DexFileDependencies']
    }
}
