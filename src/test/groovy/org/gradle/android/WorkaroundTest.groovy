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
        "3.0.0"         | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "AndroidJavaCompile_ProcessorListFile", "ExtractAnnotations_Source", "CombinedInput", "ProcessAndroidResources_MergeBlameLogFolder", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.0.1"         | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "AndroidJavaCompile_ProcessorListFile", "ExtractAnnotations_Source", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha01" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "ProcessAndroidResources_MergeBlameLogFolder", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha02" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha03" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha04" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CheckManifest_Manifest", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha05" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "DataBindingDependencyArtifacts"]
        "3.1.0-alpha06" | ["DataBindingDependencyArtifacts"]
        "3.1.0-alpha07" | ["DataBindingDependencyArtifacts"]
        "3.1.0-alpha08" | ["DataBindingDependencyArtifacts"]
        "3.1.0-alpha09" | ["DataBindingDependencyArtifacts"]
        "3.1.0-beta1"   | ["DataBindingDependencyArtifacts"]
        "3.1.0-beta2"   | ["DataBindingDependencyArtifacts"]
        "3.1.0-beta3"   | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha01" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha02" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha03" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha04" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha05" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha06" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha07" | ["DataBindingDependencyArtifacts"]
        "3.2.0-alpha08" | ["DataBindingDependencyArtifacts"]
    }
}
