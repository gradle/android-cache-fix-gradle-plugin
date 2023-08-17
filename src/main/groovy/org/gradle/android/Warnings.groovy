package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
enum Warnings {
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
