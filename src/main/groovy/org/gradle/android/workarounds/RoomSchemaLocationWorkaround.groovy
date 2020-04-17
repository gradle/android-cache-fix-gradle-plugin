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
        def project = context.project
        // Change the default room schema location to a relative path.  This avoids cacheability problems with
        // kapt tasks.  Note that kapt tasks treat all processor options as inputs, even if they are in fact
        // outputs.
        // See https://youtrack.jetbrains.com/issue/KT-31511
        def configureVariant = { variant ->
            Map<String, String> arguments = variant.javaCompileOptions.annotationProcessorOptions.arguments
            if (arguments.containsKey(ROOM_SCHEMA_LOCATION)) {
                String path = arguments.get(ROOM_SCHEMA_LOCATION)
                def schemaDir = project.relativePath(path).toString()
                arguments.put(ROOM_SCHEMA_LOCATION, schemaDir)
            }
        }

        project.plugins.withId("com.android.application") {
            def android = project.extensions.findByName("android")

            android.applicationVariants.all(configureVariant)
        }

        project.plugins.withId("com.android.library") {
            def android = project.extensions.findByName("android")

            android.libraryVariants.all(configureVariant)
        }

        // For compile tasks, capture the room.schemaLocation, add it as an output using a compiler argument
        // provider and relocate the output to a task-specific location to avoid overlapping with other tasks.
        context.compilerArgsProcessor.addRule(
            CompilerArgsProcessor.AnnotationProcessorOverride.of(ROOM_SCHEMA_LOCATION) { Task task, String path ->
                def schemaBaseDir = context.project.file(path)
                def schemaDir = new File(schemaBaseDir, task.path.replaceAll(':', '/'))
                println "Setting roomSchemaLocation to ${schemaDir}"
                task.options.compilerArgs = task.options.compilerArgs.findAll { !it.startsWith("-A${ROOM_SCHEMA_LOCATION}=")}
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
            ["-A${ROOM_SCHEMA_LOCATION}=${outputDir.path}".toString()]
        }
    }
}
