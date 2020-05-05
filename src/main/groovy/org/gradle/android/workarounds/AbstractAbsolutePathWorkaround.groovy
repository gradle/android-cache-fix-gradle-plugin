package org.gradle.android.workarounds

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.PathSensitivity

abstract class AbstractAbsolutePathWorkaround implements Workaround {
    abstract Class<? extends Task> getAndroidTaskClass()
    abstract String getPropertyName()
    abstract void setPropertyValue(Task task, FileCollection fileCollection)

    @Override
    boolean canBeApplied(Project project) {
        return true
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            FileCollection originalPropertyValue
            // Create a synthetic input with the original property value and RELATIVE path sensitivity
            project.gradle.taskGraph.beforeTask {
                if (it == task) {
                    originalPropertyValue = task.getProperty(propertyName)
                    setPropertyValue(task, project.files())
                    task.inputs.files(originalPropertyValue)
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                        .withPropertyName("${propertyName}.workaround")
                        .optional()
                }
            }
            // Set the task property back to its original value
            task.doFirst {
                setPropertyValue(task, originalPropertyValue)
            }
        }
    }
}
