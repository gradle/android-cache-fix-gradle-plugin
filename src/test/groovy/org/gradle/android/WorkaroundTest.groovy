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
        "8.1.0-alpha01" | ['MergeSourceSetFolders', 'RoomSchemaLocation', 'ZipMergingTask', 'JdkImage']
        "8.0.0-beta01"  | ['MergeSourceSetFolders', 'RoomSchemaLocation', 'ZipMergingTask', 'JdkImage']
        "7.4.0"         | ['MergeSourceSetFolders', 'RoomSchemaLocation', 'ZipMergingTask', 'JdkImage']
        "7.3.1"         | ['MergeSourceSetFolders', 'RoomSchemaLocation', 'ZipMergingTask', 'JdkImage']
        "7.2.2"         | ['MergeSourceSetFolders', 'RoomSchemaLocation', 'ZipMergingTask', 'JdkImage']
        "7.1.3"         | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'ZipMergingTask', 'JdkImage']
        "7.0.4"         | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'ZipMergingTask']
    }
}
