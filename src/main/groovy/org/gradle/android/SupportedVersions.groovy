package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@CompileStatic
class SupportedVersions {
    static final List<GradleVersion> GRADLE_VERSIONS
    static final List<VersionNumber> ANDROID_VERSIONS

    static {
        def versions = new Properties();
        def inputStream = AndroidCacheFixPlugin.classLoader.getResourceAsStream("supported-versions.properties")
        try {
            versions.load(inputStream)
        } finally {
            inputStream.close()
        }
        GRADLE_VERSIONS = versions.getProperty("gradle").split(",")
            .collect { String version -> GradleVersion.version(version) }
        ANDROID_VERSIONS = versions.getProperty("android").split(",")
            .collect { String version -> VersionNumber.parse(version) }
    }
}
