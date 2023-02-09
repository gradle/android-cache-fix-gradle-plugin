package org.gradle.android.workarounds.room

import groovy.transform.CompileStatic
import org.gradle.android.workarounds.room.androidvariants.ApplyAndroidVariants
import org.gradle.android.workarounds.room.task.RoomSchemaLocationMergeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

@CompileStatic
abstract class AnnotationProcessorWorkaround<T> {
    Project project
    RoomExtension roomExtension
    TaskProvider<RoomSchemaLocationMergeTask> mergeTask
    ApplyAndroidVariants androidVariantProvider

    AnnotationProcessorWorkaround(Project project, RoomExtension roomExtension, TaskProvider<RoomSchemaLocationMergeTask> mergeTask) {
        this.project = project
        this.roomExtension = roomExtension
        this.mergeTask = mergeTask
        this.androidVariantProvider = new ApplyAndroidVariants()

        initWorkaround()
    }

    abstract void initWorkaround()

    abstract void configureWorkaroundTask(Task task)

    /**
     * We favor ksp over kapt and apt in projects with multiple processors configured. If Ksp is applied and using
     * the room-compiler, kapt and apt workarounds won't be applied.
     * @return true if the project is applying Ksp with the room-compiler, false if not
     */
    boolean kspIsAppliedWithRoom() {
        if (project.configurations.find { it.name == "ksp" } != null) {
            boolean found = false
            project.configurations.named("ksp").get().dependencies.forEach {
                if (it.name == "room-compiler") {
                    found = true
                }
            }
            return found
        } else {
            return false
        }
    }
}
