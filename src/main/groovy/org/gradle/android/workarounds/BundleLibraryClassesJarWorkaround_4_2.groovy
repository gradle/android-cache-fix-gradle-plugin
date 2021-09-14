package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the BundleLibraryClassesJar task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "4.1.0", fixedIn = [], link = "https://issuetracker.google.com/issues/199763362")
class BundleLibraryClassesJarWorkaround_4_2 implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.BundleLibraryClassesJar.caching.enabled"

    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.internal.tasks.BundleLibraryClassesJar')
    }

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(androidTaskClass).configureEach {
            outputs.doNotCacheIf("Caching BundleLibraryClassesJar is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
