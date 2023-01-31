package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.android.VersionNumber
import org.gradle.android.workarounds.room.JavaCompileWorkaround
import org.gradle.android.workarounds.room.KaptWorkaround
import org.gradle.android.workarounds.room.KotlinVersion
import org.gradle.android.workarounds.room.RoomExtension
import org.gradle.android.workarounds.room.task.RoomSchemaLocationMergeTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

/**
 * Changes annotation processor arguments to set the room.schemaLocation via a CommandLineArgsProcessor
 * which accurately sets it as an output of the task.  Note that it is common for people to set the value
 * using the defaultConfig in the android extension, which causes every compile task to use the same
 * output directory, causing overlapping outputs and breaking cacheability again.  To avoid this, we
 * change the location for each task to write to a task-specific directory, ensuring uniqueness across
 * compile tasks.  We then add a task to merge the generated schemas back to the originally
 * specified schema directory as a post-compile step.
 *
 * This workaround adds a 'room' project extension which can be used to configure the annotation processor:
 *
 * <pre>
 *     room {
 *         schemaLocationDir.set(file("roomSchemas"))
 *     }
 * </pre>
 *
 * Note that this workaround only works with Kotlin Gradle plugin 1.3.70 or higher.
 *
 * There are multiple issues related to these problems:
 *  - https://issuetracker.google.com/issues/132245929
 *  - https://issuetracker.google.com/issues/139438151
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/132245929")
class RoomSchemaLocationWorkaround implements Workaround {
    public static final String WORKAROUND_ENABLED_PROPERTY = "org.gradle.android.cache-fix.RoomSchemaLocationWorkaround.enabled"
    public static final String ROOM_SCHEMA_LOCATION = "room.schemaLocation"
    private static final VersionNumber MINIMUM_KOTLIN_VERSION = VersionNumber.parse("1.6.0")
    private static final VersionNumber KOTLIN_VERSION = KotlinVersion.get()

    @Override
    boolean canBeApplied(Project project) {
        if (KOTLIN_VERSION != VersionNumber.UNKNOWN && KOTLIN_VERSION < MINIMUM_KOTLIN_VERSION) {
            project.logger.info("${this.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.6.0 or higher (found ${KOTLIN_VERSION.toString()}).")
            return false
        } else {
            return SystemPropertiesCompat.getBoolean(WORKAROUND_ENABLED_PROPERTY, project, true)
        }
    }

    @Override
    void apply(Project project) {
        // Project extension to hold all of the Room configuration
        def roomExtension = project.extensions.create("room", RoomExtension)

        // Create a task that will be used to merge the task-specific schema locations to the directory (or directories)
        // originally specified.  This allows us to fan out the generated output and keep good cacheability for the
        // compile/kapt tasks but still join everything later in the location the user expects.
        TaskProvider<RoomSchemaLocationMergeTask> mergeTask = project.tasks.register("mergeRoomSchemaLocations", RoomSchemaLocationMergeTask) {
            roomSchemaMergeLocations = roomExtension.roomSchemaMergeLocations
        }

        JavaCompileWorkaround javaCompileRoomTask = JavaCompileWorkaround.create(project, roomExtension, mergeTask)

        project.plugins.withId("kotlin-kapt") {
            KaptWorkaround.create(project, roomExtension, mergeTask)
            javaCompileRoomTask.javaCompileSchemaGenerationEnabled = false
        }
    }
}
