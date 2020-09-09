package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.PathSensitivity

import java.lang.reflect.Field


/**
 * Fixes the cacheability issue with CompileLibraryResourcesWorkaround where the mergedLibraryResourcesDir field is
 * treated as an input with absolute path sensitivity.
 */
@AndroidIssue(introducedIn = "4.0.0", fixedIn = ["4.2.0-alpha09"], link = "https://issuetracker.google.com/issues/155218379")
class CompileLibraryResourcesWorkaround_4_0 implements Workaround {
    // This task is new in AGP 4.0.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.tasks.CompileLibraryResourcesTask')
    }

    String propertyName = "mergedLibraryResourcesDir"

    public void setPropertyValue(Task task, DirectoryProperty directoryProperty) {
        Field field = task.class.getDeclaredFields().find { it.name == "__${propertyName}__" }
        field.setAccessible(true)
        field.set(task, directoryProperty)
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            DirectoryProperty originalPropertyValue
            // Create a synthetic input with the original property value and RELATIVE path sensitivity
            project.gradle.taskGraph.beforeTask {
                if (it == task) {
                    originalPropertyValue = task.getProperty(propertyName)
                    def dummyProperty = project.objects.directoryProperty()
                    // Non-existent file to give the DirectoryProperty a value.
                    dummyProperty.set(project.file('/doesnt-exist'))
                    setPropertyValue(task, dummyProperty)
                    task.inputs.dir(originalPropertyValue)
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

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}

