package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the BundleLibraryClasses task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["4.1.0-alpha01"], link = "https://issuetracker.google.com/issues/199763362")
class BundleLibraryClassesWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.BundleLibraryClasses.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(BundleLibraryClasses).configureEach {
            outputs.doNotCacheIf("Caching BundleLibraryClasses is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
