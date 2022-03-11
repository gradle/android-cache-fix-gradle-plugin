package org.gradle.android.workarounds

import groovy.transform.CompileStatic
import org.gradle.android.Versions
import org.gradle.api.Project
import org.gradle.util.GradleVersion

@CompileStatic
class SystemPropertiesCompat {

    /**
     * Backward-compatible boolean system property check. This allows use of new ProviderFactory methods
     * on newer Gradle versions while falling back to old APIs gracefully on older APIs.
     *
     * @param key the key to look up.
     * @param project the source gradle project. May be null.
     * @return the system property value or false if absent.
     */
    static boolean getBoolean(String key, Project project) {
        // SystemProperty was added in 6.1, but forUseAtConfigurationTime is 6.5. Since this is
        // for configuration caching, we just check on 6.5 anyway.
        if (project != null && Versions.gradle(project.gradle.gradleVersion) >= GradleVersion.version("6.5")) {
            return project.providers.systemProperty(key)
                .forUseAtConfigurationTime()
                .map {
                    try {
                        Boolean.parseBoolean(it)
                    } catch (IllegalArgumentException | NullPointerException ignored) {
                        false
                    }
                }
                .getOrElse(false)
        } else {
            return Boolean.getBoolean(key)
        }
    }
}
