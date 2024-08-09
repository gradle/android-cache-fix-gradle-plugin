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
        androidVersion  | expectedWorkarounds
        "8.7.0-alpha05" | ['JdkImage']
        "8.6.0-rc01"    | ['JdkImage']
        "8.5.1"         | ['JdkImage']
        "8.4.1"         | ['JdkImage']
        "8.3.2"         | ['MergeSourceSetFolders', 'JdkImage']
        "8.2.2"         | ['MergeSourceSetFolders', 'JdkImage', 'PackageForUnitTest']
        "8.1.4"         | ['MergeSourceSetFolders', 'JdkImage', 'PackageForUnitTest']
        "8.0.2"         | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.4.2"         | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.3.1"         | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.2.2"         | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.1.3"         | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'StripDebugSymbols', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.0.4"         | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'StripDebugSymbols', 'ZipMergingTask', 'PackageForUnitTest']
    }
}
