package org.gradle.android.workarounds

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.PathSensitivity

import java.lang.reflect.Field


abstract class AbstractCompileLibraryResourcesWorkaround_4_2_orHigher implements Workaround {

    abstract String getPropertyName();

    // This task is new in AGP 4.0.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.tasks.CompileLibraryResourcesTask')
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            ConfigurableFileCollection originalPropertyValue = project.files()
            // Create a synthetic input with the original property value and RELATIVE path sensitivity
            task.inputs.files(originalPropertyValue)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("${propertyName}.workaround")
                .optional()
            project.gradle.taskGraph.whenReady {
                def propName = propertyName
                def setPropertyValue = { ConfigurableFileCollection directoryProperty ->
                    Field field = task.class.getDeclaredFields().find { it.name == "__${propName}__" }
                    field.setAccessible(true)
                    field.set(task, directoryProperty)
                }

                originalPropertyValue.from(task.getProperty(propertyName))
                def dummyProperty = project.objects.fileCollection()
                // Non-existent file to give the ConfigurableFileCollection a value.
                dummyProperty.setFrom(project.file('/doesnt-exist'))
                setPropertyValue(dummyProperty)

                // Set the task property back to its original value
                task.doFirst {
                    setPropertyValue(originalPropertyValue)
                }
            }
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}
