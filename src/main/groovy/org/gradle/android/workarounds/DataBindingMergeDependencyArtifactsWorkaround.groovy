package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.databinding.DataBindingMergeDependencyArtifactsTask
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Disables caching of the DataBindingMergeDependencyArtifactsTask task which is mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = "7.2.0", link = "https://issuetracker.google.com/issues/200002454")
@CompileStatic
class DataBindingMergeDependencyArtifactsWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.DataBindingMergeDependencyArtifacts.caching.enabled"

    @Override
    void apply(Project project) {
        project.tasks.withType(DataBindingMergeDependencyArtifactsTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching DataBindingMergeDependencyArtifacts is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
