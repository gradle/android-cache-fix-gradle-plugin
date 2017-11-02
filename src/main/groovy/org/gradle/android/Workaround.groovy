package org.gradle.android

import org.gradle.api.Project

/**
 * Workaround to apply to an Android project. Can be annotated with {@literal @}{@link FixedInGradle} or {@literal @}{@link FixedInAndroid}.
 */
interface Workaround {
    void apply(Project project)
}
