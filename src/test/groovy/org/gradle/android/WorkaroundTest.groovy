package org.gradle.android

import spock.lang.Specification
import spock.lang.Unroll

import static Versions.android

class WorkaroundTest extends Specification {

    @Unroll
    def "applies the right workarounds for Android #androidVersion"() {
        def workarounds = AndroidCacheFixPlugin.getWorkaroundsToApply(android(androidVersion))
        expect:
        workarounds.collect { it.class.simpleName.replaceAll(/_Workaround/, "") } == expectedWorkarounds
        where:
        androidVersion  | expectedWorkarounds
        "3.0.0"         | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "AndroidJavaCompile_ProcessorListFile", "ExtractAnnotations_Source", "CombinedInput", "ProcessAndroidResources_MergeBlameLogFolder", "CheckManifest_Manifest"]
        "3.0.1"         | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "AndroidJavaCompile_ProcessorListFile", "ExtractAnnotations_Source", "CheckManifest_Manifest"]
        "3.1.0-alpha01" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "ProcessAndroidResources_MergeBlameLogFolder", "CheckManifest_Manifest"]
        "3.1.0-alpha02" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "CheckManifest_Manifest"]
        "3.1.0-alpha03" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CombinedInput", "CheckManifest_Manifest"]
        "3.1.0-alpha04" | ["AndroidJavaCompile_BootClasspath", "AndroidJavaCompile_AnnotationProcessorSource", "CheckManifest_Manifest"]
    }
}
