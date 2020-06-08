package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeJavaResourceTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

import java.lang.reflect.Method

/**
 * Fixes the cacheability issue with MergeJavaResourcesTask where the projectJavaRes field is treated as an input with
 * absolute path sensitivity.  This mostly comes up when the kotlin plugin has been applied, which puts the
 * kotlin_module files into this input.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = ["4.1.0-alpha09"], link = "https://issuetracker.google.com/issues/140602655")
class MergeJavaResourcesWorkaround extends AbstractAbsolutePathWorkaround {
    Class<?> androidTaskClass = MergeJavaResourceTask.class
    String propertyName = "projectJavaRes"

    public void setPropertyValue(Task task, FileCollection fileCollection) {
        Method method = task.class.superclass.getMethods().find { it.name == "access\$set${propertyName.capitalize()}\$p" }
        method.setAccessible(true)
        method.invoke(task, task, fileCollection)
    }
}
