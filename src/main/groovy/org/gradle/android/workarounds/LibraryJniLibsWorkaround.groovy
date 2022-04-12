package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.LibraryJniLibsTask;
import groovy.transform.CompileStatic;
import org.gradle.android.AndroidIssue;
import org.gradle.api.Project
import org.gradle.api.Task;

/**
 * Disables caching of the LibraryJniLibsTask task which is mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "3.6.0", fixedIn = "7.2.0-alpha06", link = "https://issuetracker.google.com/issues/200002454")
@CompileStatic
class LibraryJniLibsWorkaround implements Workaround {

    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.LibraryJniLibs.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(LibraryJniLibsTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching LibraryJniLibs is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
