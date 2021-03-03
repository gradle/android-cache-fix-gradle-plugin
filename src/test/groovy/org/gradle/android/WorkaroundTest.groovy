package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

class WorkaroundTest extends Specification {
    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def possibleWorkarounds = AndroidCacheFixPlugin.initializeWorkarounds(null)
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(Versions.android(androidVersion), null, possibleWorkarounds)
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/Workaround/, "") }.sort() == expectedWorkarounds.sort()
        where:
        androidVersion  | expectedWorkarounds
        "7.0.0-alpha01" | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_2', 'MergeResources']
        "4.2.0-beta05"  | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_2', 'MergeResources']
        "4.1.2"         | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_0', 'MergeResources']
        "4.0.2"         | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_0', 'MergeResources']
        "3.6.4"         | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs']
        "3.5.4"         | ['MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs']
    }
}
