package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.PathSensitivity

/**
 * Fixes the cacheability issue with MergeResources Task where the rawLocalResources field is treated as an input with
 * absolute path sensitivity.
 */
@AndroidIssue(introducedIn = "4.0.0", fixedIn = [], link = "https://issuetracker.google.com/issues/141301405")
class MergeResourcesWorkaround implements Workaround {
    // This task is new in AGP 4.0.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.tasks.MergeResources')
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            Map<String, FileCollection> originalResources = [:]
            // Create a synthetic input with the original value and RELATIVE path sensitivity
            project.gradle.taskGraph.beforeTask {
                if (it == task) {
                    originalResources.putAll(task.resourcesComputer.resources)
                    task.resourcesComputer.resources.clear()
                    task.inputs.files(originalResources.values())
                            .withPathSensitivity(PathSensitivity.RELATIVE)
                            .withPropertyName("rawLocalResources.workaround")
                }
            }
            // Set the source back to its original value before we execute the main task action
            task.doFirst {
                task.resourcesComputer.resources.putAll(originalResources)
            }
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
