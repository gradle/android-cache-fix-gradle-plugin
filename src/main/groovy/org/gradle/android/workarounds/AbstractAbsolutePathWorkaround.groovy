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
            FileCollection originalPropertyValue = project.files()
            // Create a synthetic input with the original property value and RELATIVE path sensitivity
            task.inputs.files(originalPropertyValue)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("${propertyName}.workaround")
                .optional()
            project.gradle.taskGraph.beforeTask {
                if (it == task) {
                    originalPropertyValue.from(task.getProperty(propertyName))
                    setPropertyValue(task, project.files())
                }
            }
            // Set the task property back to its original value
            task.doFirst {
                setPropertyValue(task, originalPropertyValue)
            }
        }
    }
}
