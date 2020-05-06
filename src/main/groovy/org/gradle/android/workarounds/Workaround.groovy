package org.gradle.android.workarounds

import org.gradle.api.Project

/**
 * Workaround to apply to an Android project. Can be annotated with {@literal @}{@link AndroidIssue}.
 */
interface Workaround {
    void apply(WorkaroundContext context)
    boolean canBeApplied(Project project)
}
