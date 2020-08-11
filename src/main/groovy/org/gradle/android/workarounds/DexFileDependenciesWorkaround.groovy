package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.DexFileDependenciesTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/160138798")
class DexFileDependenciesWorkaround implements Workaround {
    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(DexFileDependenciesTask).configureEach {
            outputs.cacheIf {true }
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
