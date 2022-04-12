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
        "7.3.0-alpha07" | ['ZipMergingTask', 'LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources']
        "7.2.0-beta04" | ['ZipMergingTask', 'LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources']
        "7.1.3"  | ['ZipMergingTask', 'LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources']
        "7.0.4"         | ['ZipMergingTask', 'LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources']
    }
}
