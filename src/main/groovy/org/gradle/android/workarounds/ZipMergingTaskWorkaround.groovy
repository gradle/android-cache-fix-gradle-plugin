package org.gradle.android.workarounds

import com.android.build.gradle.tasks.ZipMergingTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of ZipMergingTask which is mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link="https://issuetracker.google.com/issues/200002454")
class ZipMergingTaskWorkaround implements Workaround {

    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.ZipMergingTask.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(ZipMergingTask).configureEach {
            outputs.doNotCacheIf("Caching ZipMergingTask is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
