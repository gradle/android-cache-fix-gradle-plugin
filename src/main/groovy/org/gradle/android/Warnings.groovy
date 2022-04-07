package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
enum Warnings {
    MAYBE_SUPPORTED_ANDROID_VERSION("WARNING: Android plugin ${Versions.CURRENT_ANDROID_VERSION} has not been tested with this version of the Android cache fix plugin, although it may work.  We test against only the latest patch release versions of Android Gradle plugin: ${Versions.SUPPORTED_ANDROID_VERSIONS.join(", ")}.  If ${Versions.CURRENT_ANDROID_VERSION} is newly released, we may not have had a chance to release a version tested against it yet.  Proceed with caution.  You can suppress this warning with with -D${Versions.IGNORE_VERSION_CHECK_PROPERTY}=true."),
    USE_COMPILE_LIBRARY_RESOURCES_EXPERIMENTAL("WARNING: Android plugin ${Versions.CURRENT_ANDROID_VERSION} has experimental support for using relative path sensitivity with CompileLibraryResourcesTask inputs which will provide more build cache hits and improve build speed.  Set '${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=true' and '${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=true' in gradle.properties to enable this support.")

    final String warning
    private final AtomicBoolean warned = new AtomicBoolean()

    Warnings(String warning) {
        this.warning = warning
    }

    void warnOnce(Project project) {
        if (isNotKotlinDslAccessors(project) && !warned.getAndSet(true)) {
            project.logger.warn(warning)
        }
    }

    void reset() {
        warned.set(false)
    }

    static void resetAll() {
        values().each {it.reset() }
    }

    static boolean isNotKotlinDslAccessors(Project project) {
        return project.rootProject.name != "gradle-kotlin-dsl-accessors"
    }
}
