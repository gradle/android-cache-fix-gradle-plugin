package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.PackageForUnitTest
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

@AndroidIssue(introducedIn = "3.5.0-alpha05", fixedIn = "8.3.0-alpha01", link = "https://issuetracker.google.com/issues/292114808")
@CompileStatic
class PackageForUnitTestWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.PackageForUnitTest.caching.enabled"

    @Override
    void apply(Project project) {
        project.tasks.withType(PackageForUnitTest).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching PackageForUnitTest is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
