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

    boolean kspIsAppliedWithRoom() {
        if (project.configurations.find { it.name == "ksp" } != null) {
            boolean found = false
            project.configurations.findByName("ksp").dependencies.forEach {
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
