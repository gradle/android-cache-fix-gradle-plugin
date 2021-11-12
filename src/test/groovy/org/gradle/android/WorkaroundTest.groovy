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
        "7.2.0-alpha04" | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses_4_2', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_7_0']
        "7.1.0-beta03"  | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses_4_2', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_7_0']
        "7.0.3"         | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses_4_2', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_7_0']
        "4.2.2"         | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses_4_2', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_2', 'MergeResources']
        "4.1.3"         | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses_4_2', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_0', 'MergeResources']
        "4.0.2"         | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs', 'CompileLibraryResources_4_0', 'MergeResources']
        "3.6.4"         | ['LibraryJniLibs', 'DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs']
        "3.5.4"         | ['DataBindingMergeDependencyArtifacts', 'BundleLibraryClasses', 'MergeSourceSetFolders', 'MergeJavaResources', 'RoomSchemaLocation', 'StripDebugSymbols', 'MergeNativeLibs']
    }
}
