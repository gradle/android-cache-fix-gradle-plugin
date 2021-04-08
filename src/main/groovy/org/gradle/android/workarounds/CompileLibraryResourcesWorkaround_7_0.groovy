package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue

/**
 * Fixes the cacheability issue with CompileLibraryResourcesWorkaround where the inputDirectories field is
 * treated as an input with absolute path sensitivity.
 * This is the same as the {@link CompileLibraryResourcesWorkaround_4_0} but the field was renamed with a new type
 * in 4.2.0-alpha09.
 */
@AndroidIssue(introducedIn = "7.0.0-alpha09", fixedIn = [], link = "https://issuetracker.google.com/issues/155218379")
class CompileLibraryResourcesWorkaround_7_0 extends AbstractCompileLibraryResourcesWorkaround_4_2_orHigher {
    @Override
    String getPropertyName() {
        return "inputDirectoriesAsAbsolute"
    }
}
