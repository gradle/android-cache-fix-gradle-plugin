package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.android.Warnings
import org.gradle.api.Project

/**
 * Warns if the user is not using experimental support for relative path sensitivity that was added
 * with 7.0.0-alpha09.
 */
@AndroidIssue(introducedIn = "7.0.0-alpha09", fixedIn = [], link = "https://issuetracker.google.com/issues/155218379")
class CompileLibraryResourcesWorkaround implements Workaround {
    public static final String ENABLE_SOURCE_SET_PATHS_MAP = "android.experimental.enableSourceSetPathsMap"
    public static final String CACHE_COMPILE_LIB_RESOURCES = "android.experimental.cacheCompileLibResources"

    @Override
    void apply(WorkaroundContext context) {
        boolean enableSourceSetPathsMap = Boolean.valueOf(context.project.findProperty(ENABLE_SOURCE_SET_PATHS_MAP) as String)
        boolean cacheCompileLibResources = Boolean.valueOf(context.project.findProperty(CACHE_COMPILE_LIB_RESOURCES) as String)

        if (!(enableSourceSetPathsMap && cacheCompileLibResources)) {
            Warnings.USE_COMPILE_LIBRARY_RESOURCES_EXPERIMENTAL.warnOnce(context.project.logger)
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
