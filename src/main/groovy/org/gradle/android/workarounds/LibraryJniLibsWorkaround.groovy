package org.gradle.android.workarounds;

import groovy.transform.CompileStatic;
import org.gradle.android.AndroidIssue;
import org.gradle.api.Project
import org.gradle.api.Task;

/**
 * Disables caching of the LibraryJniLibsTask task which is mostly disk bound and
 * unlikely to provide positive performance benefits.
 */
@AndroidIssue(introducedIn = "3.6.0", fixedIn = [], link = "https://issuetracker.google.com/issues/200002454")
@CompileStatic
class LibraryJniLibsWorkaround implements Workaround {
    // This task is new in AGP 3.6.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.internal.tasks.LibraryJniLibsTask')
    }

    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.LibraryJniLibs.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(androidTaskClass).configureEach {Task task ->
            task.outputs.doNotCacheIf("Caching LibraryJniLibs is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
