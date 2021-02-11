package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/153088766")
class MergeNativeLibsWorkaround implements Workaround {
    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeNativeLibsTask).configureEach {
            outputs.doNotCacheIf("Caching MergeNativeLibs adds overhead to the build and never saves time", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
