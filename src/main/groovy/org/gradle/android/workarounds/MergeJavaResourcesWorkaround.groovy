package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

import java.lang.reflect.Method

import static org.gradle.api.tasks.PathSensitivity.*

@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["4.0.1"], link = "https://issuetracker.google.com/issues/140602655")
class MergeJavaResourcesWorkaround implements Workaround {
    @Override
    void apply(WorkaroundContext context) {
        Project project = context.project
        project.tasksWithType(MergeJavaResourceTask).configureEach { Task task ->
            FileCollection originalJavaResources
            // Create a synthetic input with the original java resources and RELATIVE path sensitivity
            project.gradle.taskGraph.beforeTask {
                if (it == task) {
                    originalJavaResources = task.projectJavaRes
                    setJavaRes(task, project.files())
                    task.inputs.files(originalJavaResources)
                        .withPathSensitivity(RELATIVE)
                        .withPropertyName("projectJavaRes.workaround")
                        .skipWhenEmpty(true)
                }
            }
            // Set the task property back to its original value
            task.doFirst {
                setJavaRes(task, originalJavaResources)
            }
        }
    }

    private void setJavaRes(Task task, FileCollection fileCollection) {
        Method method = task.class.superclass.getMethods().find { it.name == "access\$setProjectJavaRes\$p" }
        method.setAccessible(true)
        method.invoke(task, task, fileCollection)
    }
}
