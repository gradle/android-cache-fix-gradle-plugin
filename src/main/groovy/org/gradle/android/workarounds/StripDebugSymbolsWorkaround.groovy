package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.StripDebugSymbolsTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project

/**
 * Disables caching of the StripDebugSymbols task which is mostly disk bound and unlikely to provide positive
 * performance benefits.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/181143775")
class StripDebugSymbolsWorkaround implements Workaround {
    private static final String CACHING_ENABLED_PROPERTY = "org.gradle.android.cache-fix.StripDebugSymbols.caching.enabled"

    @Override
    void apply(WorkaroundContext context) {
        context.project.tasks.withType(StripDebugSymbolsTask).configureEach {
            outputs.doNotCacheIf("Caching StripDebugSymbolsTask is unlikely to provide positive performance results.", {true })
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return !SystemPropertiesCompat.getBoolean(CACHING_ENABLED_PROPERTY, project)
    }
}
