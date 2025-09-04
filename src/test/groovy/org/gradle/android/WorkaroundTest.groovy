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
        androidVersion | expectedWorkarounds
        "9.0"          | ['JdkImage']
        "8.13"         | ['JdkImage']
        "8.12"         | ['JdkImage']
        "8.11"         | ['JdkImage']
        "8.10"         | ['JdkImage']
        "8.9"          | ['JdkImage']
        "8.8"          | ['JdkImage']
        "8.7"          | ['JdkImage']
        "8.6"          | ['JdkImage']
        "8.5"          | ['JdkImage']
        "8.4"          | ['JdkImage']
        "8.3"          | ['MergeSourceSetFolders', 'JdkImage']
        "8.2"          | ['MergeSourceSetFolders', 'JdkImage', 'PackageForUnitTest']
        "8.1"          | ['MergeSourceSetFolders', 'JdkImage', 'PackageForUnitTest']
        "8.0"          | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.4"          | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.3"          | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.2"          | ['MergeSourceSetFolders', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.1"          | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'StripDebugSymbols', 'ZipMergingTask', 'JdkImage', 'PackageForUnitTest']
        "7.0"          | ['BundleLibraryClasses', 'CompileLibraryResources', 'DataBindingMergeDependencyArtifacts', 'LibraryJniLibs', 'MergeNativeLibs', 'MergeSourceSetFolders', 'StripDebugSymbols', 'ZipMergingTask', 'PackageForUnitTest']
    }
}
