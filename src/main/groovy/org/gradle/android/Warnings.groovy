package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

@CompileStatic
enum Warnings {
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
