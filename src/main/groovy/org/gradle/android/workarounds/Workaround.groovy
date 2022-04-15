package org.gradle.android.workarounds

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * Workaround to apply to an Android project. Can be annotated with {@literal @}{@link org.gradle.android.AndroidIssue}.
 */
@CompileStatic
interface Workaround {
    void apply(Project project)
    boolean canBeApplied(Project project)
}
