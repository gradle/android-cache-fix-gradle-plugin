package org.gradle.android.workarounds

import com.android.build.gradle.tasks.MergeSourceSetFolders
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Disables caching of the MergeSourceSetFolders task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@CompileStatic
@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["8.4.0-alpha01"], link = "https://issuetracker.google.com/issues/194804421")
class MergeSourceSetFoldersWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeSourceSetFolders.caching.enabled"

    @Override
    void apply(Project project) {
        project.tasks.withType(MergeSourceSetFolders).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching MergeSourceSetFolders is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
