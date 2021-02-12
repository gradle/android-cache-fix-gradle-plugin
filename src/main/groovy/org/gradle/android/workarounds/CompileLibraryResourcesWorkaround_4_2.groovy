package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.PathSensitivity

import java.lang.reflect.Field


/**
 * Fixes the cacheability issue with CompileLibraryResourcesWorkaround where the inputDirectories field is
 * treated as an input with absolute path sensitivity.
 * This is the same as the {@link CompileLibraryResourcesWorkaround_4_0} but the field was renamed with a new type
 * in 4.2.0-alpha09.
 */
@AndroidIssue(introducedIn = "4.2.0-alpha09", fixedIn = [], link = "https://issuetracker.google.com/issues/155218379")
class CompileLibraryResourcesWorkaround_4_2 implements Workaround {
    // This task is new in AGP 4.0.0 so use Class.forName to allow for backward compatibility with older AGP versions.
    static Class<?> getAndroidTaskClass() {
        return Class.forName('com.android.build.gradle.tasks.CompileLibraryResourcesTask')
    }

    static final String PROPERTY_NAME = "inputDirectories"

    public static void setPropertyValue(Task task, ConfigurableFileCollection directoryProperty) {
        Field field = task.class.getDeclaredFields().find { it.name == "__${PROPERTY_NAME}__" }
        field.setAccessible(true)
        field.set(task, directoryProperty)
    }

    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasks.withType(androidTaskClass).configureEach { Task task ->
            ConfigurableFileCollection originalPropertyValue = project.files()
            // Create a synthetic input with the original property value and RELATIVE path sensitivity
            task.inputs.files(originalPropertyValue)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("${PROPERTY_NAME}.workaround")
                .optional()
            project.gradle.taskGraph.whenReady {
                originalPropertyValue.from(task.getProperty(PROPERTY_NAME))
                def dummyProperty = project.objects.fileCollection()
                // Non-existent file to give the ConfigurableFileCollection a value.
                dummyProperty.setFrom(project.file('/doesnt-exist'))
                setPropertyValue(task, dummyProperty)

                // Set the task property back to its original value
                task.doFirst {
                    setPropertyValue(it, originalPropertyValue)
                }

            }
        }
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }
}

