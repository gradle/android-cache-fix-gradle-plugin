package org.gradle.android.workarounds

import groovy.transform.CompileStatic
import org.gradle.android.Versions
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

import java.lang.reflect.Method

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
        return getBoolean(key, project, false)
    }

    /**
     * Backward-compatible boolean system property check. This allows use of new ProviderFactory methods
     * on newer Gradle versions while falling back to old APIs gracefully on older APIs.
     *
     * @param key the key to look up.
     * @param project the source gradle project. May be null.
     * @param the default value to return if the value is absent
     * @return the system property value or default value if absent.
     */
    static boolean getBoolean(String key, Project project, Boolean absentValue) {
        if (project != null) {
            getSystemProperty(key, project).map {
                    try {
                        return Boolean.parseBoolean(it)
                    } catch (IllegalArgumentException | NullPointerException ignored) {
                        return absentValue
                    }
                }
                .getOrElse(absentValue)
        } else {
            return Boolean.getBoolean(key)
        }
    }

    static String getString(String key, Project project, String absentValue) {
        if (project != null) {
            getSystemProperty(key, project).getOrElse(absentValue)
        } else {
            return System.getProperty(key)
        }
    }

    private static Provider<String> getSystemProperty(String key, Project project) {
        return forUseAtConfigurationTime(project.providers.systemProperty(key), project)
    }

    private static Provider<String> forUseAtConfigurationTime(Provider<String> provider, Project project) {
        if (Versions.gradle(project.gradle.gradleVersion) < GradleVersion.version("7.4")) {
            try {
                // Use reflection to access the forUseAtConfigurationTime method as it was removed in Gradle 9.
                Method method = Provider.class.getMethod("forUseAtConfigurationTime");
                return (Provider<String>) method.invoke(provider);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke forUseAtConfigurationTime via reflection", e);
            }
        } else {
            return provider;
        }
    }
}
