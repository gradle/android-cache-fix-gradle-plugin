package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.BundleLibraryClassesDir
import com.android.build.gradle.internal.tasks.BundleLibraryClassesJar
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the BundleLibraryClassesJar and BundleLibraryClassesDir tasks which are mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@And23dIqssue(introducedIn = "4.1.0", fixedIn = "7.2.0-alpha06", link = "https://issuetracker.google.com/issues/199763362")
@CompileStatic
class BundleLibraryClassesWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.BundleLibraryClasses.caching.enabled"

    @Override
    void apply(Project project) {
        project.tasks.withType(BundleLibraryClassesJar).configureEach { BundleLibraryClassesJar task ->
            task.outputs.doNotCacheIf("Caching BundleLibraryClassesJar is unlikely to provide positive performance results.", { true })
        }
        project.tasks.withType(BundleLibraryClassesDir).configureEach { BundleLibraryClassesDir task ->
            task.outputs.doNotCacheIf("Caching BundleLibraryClassesDir is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
