package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/153088766")
class MergeNativeLibsWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.MergeNativeLibs.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(MergeNativeLibsTask).configureEach {
            outputs.doNotCacheIf("Caching MergeNativeLibs is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
