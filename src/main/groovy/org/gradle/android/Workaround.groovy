package org.gradle.android

import org.gradle.api.Project

/**
 * Workaround to apply to an Android project. Can be annotated with {@literal @}{@link AndroidIssue}.
 */
interface Workaround {
    void apply(Project project)
}
