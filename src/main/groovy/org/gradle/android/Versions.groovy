package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@CompileStatic
class Versions {
    static final VersionNumber PLUGIN_VERSION;
    static final List<GradleVersion> SUPPORTED_GRADLE_VERSIONS
    static final List<VersionNumber> SUPPORTED_ANDROID_VERSIONS

    static {
        def versions = new Properties();
        def inputStream = AndroidCacheFixPlugin.classLoader.getResourceAsStream("versions.properties")
        try {
            versions.load(inputStream)
        } finally {
            inputStream.close()
        }
        PLUGIN_VERSION = VersionNumber.parse(versions.getProperty("version"))
        SUPPORTED_GRADLE_VERSIONS = versions.getProperty("gradleVersions").split(",")
            .collect { String version -> GradleVersion.version(version) }
        SUPPORTED_ANDROID_VERSIONS = versions.getProperty("androidVersions").split(",")
            .collect { String version -> VersionNumber.parse(version) }
    }
}
