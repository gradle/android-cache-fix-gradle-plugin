package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the BundleLibraryClasses task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["4.2.0-alpha09"], link = "https://issuetracker.google.com/issues/199763362")
class BundleLibraryClassesJarWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.BundleLibraryClassesJar.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(BundleLibraryClasses).configureEach {
            outputs.doNotCacheIf("Caching BundleLibraryClassesJar is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
