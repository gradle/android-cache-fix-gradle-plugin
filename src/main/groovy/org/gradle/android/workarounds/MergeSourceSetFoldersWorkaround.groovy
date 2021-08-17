package org.gradle.android.workarounds

import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the MergeSourceSetFolders task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/194804421")
class MergeSourceSetFoldersWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeSourceSetFolders.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeSourceSetFolders).configureEach {
            outputs.doNotCacheIf("Caching MergeSourceSetFolders is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
