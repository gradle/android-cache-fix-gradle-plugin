package org.gradle.android.workarounds

import com.android.build.gradle.tasks.ZipMergingTask
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Disables caching of ZipMergingTask which is mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link="https://issuetracker.google.com/issues/200002454")
@CompileStatic
class ZipMergingTaskWorkaround implements Workaround {

    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.ZipMergingTask.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(ZipMergingTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching ZipMergingTask is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
