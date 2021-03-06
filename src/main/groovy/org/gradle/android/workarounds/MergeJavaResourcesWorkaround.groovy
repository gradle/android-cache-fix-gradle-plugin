package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the MergeJavaResources task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/181142260")
class MergeJavaResourcesWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeJavaResources.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeJavaResourceTask).configureEach {
            outputs.doNotCacheIf("Caching MergeJavaResources is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
