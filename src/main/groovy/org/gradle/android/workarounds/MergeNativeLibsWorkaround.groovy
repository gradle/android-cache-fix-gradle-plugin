package org.gradle.android.workarounds

import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import org.gradle.android.AndroidIssue
import org.gradle.api.Task
import org.gradle.api.file.FileCollection

import java.lang.reflect.Field

/**
 * Fixes the cacheability issue with MergeNativeLibsTask where the projectNativeLibs field is treated as an input with
 * absolute path sensitivity.  This mostly comes up when either native libraries are built with the application
 * or RenderScript files are compiled as part of the build.
 *
 * In 3.5.x, projectNativeLibs is not directly backed by state in the Task object.  Instead, it is effectively a
 * method that calculates a new FileCollection from variantScope every time it is called.  So we only
 * support this workaround for 3.6.x and 4.0.x.
 */
@AndroidIssue(introducedIn = "3.6.0", fixedIn = ["4.1.0-alpha09"], link = "https://issuetracker.google.com/issues/140602655")
class MergeNativeLibsWorkaround extends AbstractAbsolutePathWorkaround {
    Class<?> androidTaskClass = MergeNativeLibsTask.class
    String propertyName = "projectNativeLibs"

    @Override
    void apply(WorkaroundContext context) {
        super.apply(context)
    }

    public void setPropertyValue(Task task, FileCollection fileCollection) {
        // The projectNativeLibs input is a decorated field without a setter in 3.6.x and 4.0.x
        Field field = task.class.getDeclaredFields().find { it.name == "__${propertyName}__" }

        if (field != null) {
            field.setAccessible(true)
            field.set(task, fileCollection)
        }
    }
}
