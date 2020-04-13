package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

import java.lang.reflect.Method

@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["4.1.0"], link = "https://issuetracker.google.com/issues/140602655")
class MergeJavaResourcesWorkaround extends AbstractAbsolutePathWorkaround {
    Class<?> androidTaskClass = MergeJavaResourceTask.class
    String propertyName = "projectJavaRes"

    public void setPropertyValue(Task task, FileCollection fileCollection) {
        Method method = task.class.superclass.getMethods().find { it.name == "access\$set${propertyName.capitalize()}\$p" }
        method.setAccessible(true)
        method.invoke(task, task, fileCollection)
    }
}
