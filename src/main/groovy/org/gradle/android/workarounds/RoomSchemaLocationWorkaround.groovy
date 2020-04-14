package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Task
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.CommandLineArgumentProvider

/**
 * Changes annotation processor arguments to set the room.schemaLocation via a CommandLineArgsProcessor
 * which accurately sets it as an output of the task.  Note that it is common for people to set the value
 * using the defaultConfig in the android extension, which causes every compile task to use the same
 * output directory, causing overlapping outputs and breaking cacheability again.  To avoid this, we
 * change the location for each task to treat the specified directory as a "base" directory, but derive
 * the task-specific directory off of the task path, ensuring uniqueness across compile tasks.
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/132245929")
class RoomSchemaLocationWorkaround implements Workaround {
    private static final String ROOM_SCHEMA_LOCATION = "room.schemaLocation"

    @Override
    void apply(WorkaroundContext context) {
        context.compilerArgsProcessor.addRule(
            CompilerArgsProcessor.AnnotationProcessorOverride.of(ROOM_SCHEMA_LOCATION) { Task task, String path ->
                def schemaBaseDir = context.project.file(path)
                def schemaDir = new File(schemaBaseDir, task.path.replaceAll(':', '/'))
                task.options.compilerArgumentProviders.add(
                    new RoomSchemaLocationArgsProvider(schemaDir)
                )
            }
        )
    }

    class RoomSchemaLocationArgsProvider implements CommandLineArgumentProvider {
        @OutputDirectory
        File outputDir

        RoomSchemaLocationArgsProvider(File output) {
            outputDir = output
        }

        @Override
        Iterable<String> asArguments() {
            ["-A${ROOM_SCHEMA_LOCATION}=${outputDir.path}"]
        }
    }
}
