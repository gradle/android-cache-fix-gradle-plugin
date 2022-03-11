package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/153088766")
@CompileStatic
class MergeNativeLibsWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeNativeLibs.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeNativeLibsTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching MergeNativeLibs is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
