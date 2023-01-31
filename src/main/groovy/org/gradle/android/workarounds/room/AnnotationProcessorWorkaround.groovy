package org.gradle.android.workarounds.room

import org.gradle.android.workarounds.room.androidvariants.ApplyAndroidVariants
import org.gradle.android.workarounds.room.task.RoomSchemaLocationMergeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

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
}
