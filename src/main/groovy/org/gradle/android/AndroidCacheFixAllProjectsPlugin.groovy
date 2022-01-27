package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class AndroidCacheFixAllProjectsPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // Check to ensure that we are in the root project
        if (project.getParent() != null) {
            throw new IllegalStateException("AndroidCacheFixAllProjectsPlugin should only be applied to the root project")
        }

        // Apply the plugin to all Android sub-projects
        project.getAllprojects().each { subproject ->
            subproject.getPlugins().withId("com.android.base") {
                subproject.getPluginManager().apply(AndroidCacheFixPlugin.class)
            }
        }
    }
}
