package org.gradle.android

import com.android.Version
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final VersionNumber CURRENT_ANDROID_VERSION = android(Version.ANDROID_GRADLE_PLUGIN_VERSION)
    static final VersionNumber MINIMUM_ANDROID_VERSION = android("9.0.0-alpha01")

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static boolean isSupportedAndroidVersion() {
        return CURRENT_ANDROID_VERSION >= MINIMUM_ANDROID_VERSION
    }
}
