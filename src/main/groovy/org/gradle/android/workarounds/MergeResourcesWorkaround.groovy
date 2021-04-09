package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.PathSensitivity

/**
 * Fixes the cacheability issue with MergeResources Task where the rawLocalResources field is treated as an input with
 * absolute path sensitivity.
 *
 * This is marked as "fixedIn" AGP 7.0.0-alpha09, but the issue has not been fixed.  That version introduced changes
 * that prevent us from working around the issue anymore, so we can no longer apply it.
 */
@AndroidIssue(introducedIn = "4.0.0", fixedIn = ['7.0.0-alpha09'], link = "https://issuetracker.google.com/issues/141301405")
class MergeResourcesWorkaround implements Workaround {
    // This task is new in AGP 4.0.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.tasks.MergeResources')
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            MapProperty<String, FileCollection> originalResources = project.objects.mapProperty(String, FileCollection)
            // Create a synthetic input with the original value and RELATIVE path sensitivity
            task.inputs.files(originalResources.map {it.values() })
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("rawLocalResources.workaround")
            project.gradle.taskGraph.whenReady { TaskExecutionGraph executionGraph ->
                task.resourcesComputer.resources.each { key, value -> originalResources.put(key, value) }
                task.resourcesComputer.resources.clear()
                // Set the source back to its original value before we execute the main task action
                task.doFirst {
                    it.resourcesComputer.resources.putAll(originalResources.get())
                }
            }
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
