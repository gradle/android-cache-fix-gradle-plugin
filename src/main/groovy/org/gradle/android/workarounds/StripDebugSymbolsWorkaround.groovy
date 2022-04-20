package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.StripDebugSymbolsTask
import groovy.transform.CompileStatic
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Disables caching of the StripDebugSymbols task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = "7.2.0-alpha06", link = "https://issuetracker.google.com/issues/181143775")
@CompileStatic
class StripDebugSymbolsWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.StripDebugSymbols.caching.enabled"

    @Override
    void apply(Project project) {
        project.tasks.withType(StripDebugSymbolsTask).configureEach { Task task ->
            task.outputs.doNotCacheIf("Caching StripDebugSymbolsTask is unlikely to provide positive performance results.", { true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
