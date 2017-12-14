package org.gradle.android
/**
 * Workaround to apply to an Android project. Can be annotated with {@literal @}{@link AndroidIssue}.
 */
interface Workaround {
    void apply(WorkaroundContext context)
}
