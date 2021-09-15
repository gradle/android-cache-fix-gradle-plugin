package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the BundleLibraryClassesJar and BundleLibraryClassesDir tasks which are mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "4.1.0", fixedIn = [], link = "https://issuetracker.google.com/issues/199763362")
class BundleLibraryClassesWorkaround_4_2 implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.BundleLibraryClasses.caching.enabled"

    static Class<?> getJarTaskClass() {
        return Class.forName('com.android.build.gradle.internal.tasks.BundleLibraryClassesJar')
    }

    static Class<?> getDirTaskClass() {
        return Class.forName('com.android.build.gradle.internal.tasks.BundleLibraryClassesDir')
    }


    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(jarTaskClass).configureEach {
            outputs.doNotCacheIf("Caching BundleLibraryClassesJar is unlikely to provide positive performance results.", {true })
        }
        context.project.tasks.withType(dirTaskClass).configureEach {
            outputs.doNotCacheIf("Caching BundleLibraryClassesDir is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
