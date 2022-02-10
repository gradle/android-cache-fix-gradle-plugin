package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Disables caching of the MergeJavaResources task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/181142260")
@CompileStatic
class MergeJavaResourcesWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeJavaResources.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeJavaResourceTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching MergeJavaResources is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
