package org.gradle.android.workarounds

import kotlin.InitializedLazyImpl
import org.gradle.android.AndroidIssue
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

/**
 * Changes Java annotation processor arguments to set the room.schemaLocation via a CommandLineArgsProcessor
 * which accurately sets it as an output of the task.  Note that it is common for people to set the value
 * using the defaultConfig in the android extension, which causes every compile task to use the same
 * output directory, causing overlapping outputs and breaking cacheability again.  To avoid this, we
 * change the location for each task to write to a task-specific directory, ensuring uniqueness across
 * compile tasks.  We then add a task to merge the generated schemas back to the originally
 * specified schema directory as a post-compile step.
 *
 * For kapt tasks, we cannot implement a CommandLineArgsProcessor, so we change the value to be relative
 * for the purposes of snapshotting inputs, then change it to the absolute path of a task-specific directory
 * right before the task executes.  We then merge it back to the originally specified directory using the
 * aforementioned merge task.
 *
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/132245929")
class RoomSchemaLocationWorkaround implements Workaround {
    private static final String ROOM_SCHEMA_LOCATION = "room.schemaLocation"
    private static final String KAPT_USE_WORKER_API = "kapt.use.worker.api"
    private static final String KAPT_SUBPLUGIN_ID = "org.jetbrains.kotlin.kapt3"

    @Override
    void apply(WorkaroundContext context) {
        def project = context.project

        // When kapt.use.worker.api=false, the processor options are Base64 encoded and it is sufficiently difficult
        // to unpack, change and repack them that we just punt on this scenario for now.
        if (project.hasProperty(KAPT_USE_WORKER_API) && project.property(KAPT_USE_WORKER_API) == "false") {
            project.logger.lifecycle("RoomSchemaLocationWorkaround only works when ${KAPT_USE_WORKER_API} is set to true.  Ignoring.")
            return
        }

        // Create a task that will be used to merge the task-specific schema locations to the directory (or directories)
        // originally specified.  This allows us to fan out the generated output and keep good cacheability for the
        // compile/kapt tasks but still join everything later in the location the user expects.
        project.ext.roomSchemaMergeLocations = new MergeAssociations(project.objects)
        TaskProvider<RoomSchemaLocationMergeTask> mergeTask = project.tasks.register("mergeRoomSchemaLocations", RoomSchemaLocationMergeTask) {
            roomSchemaMergeLocations = project.roomSchemaMergeLocations
        }

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
                project.tasks.withType(JavaCompile).configureEach { finalizedBy mergeTask }
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
                def schemaDestinationDir = context.project.file(path)
                def taskSpecificSchemaDir = getTaskSpecificSchemaDir(task)
                task.options.compilerArgs = task.options.compilerArgs.findAll { !it.startsWith("-A${ROOM_SCHEMA_LOCATION}=")}
                task.options.compilerArgumentProviders.add(
                    new RoomSchemaLocationArgsProvider(taskSpecificSchemaDir)
                )

                // Register the generated schemas to be merged back to the original specified schema directory
                task.project.roomSchemaMergeLocations.registerMerge(schemaDestinationDir, taskSpecificSchemaDir)
            }
        )

        // Change the room schema location back to an absolute path right before the kapt tasks execute.
        // This allows other annotation processors that rely on the path being absolute to still function.
        project.plugins.withId("kotlin-kapt") {
            project.tasks.withType(kaptWithoutKotlincTaskClass) { Task task ->
                task.finalizedBy mergeTask

                project.gradle.taskGraph.beforeTask {
                    if (it == task) {
                        configureSchemaLocationOutputs(task)
                    }
                }

                doFirst {
                    setKaptRoomSchemaLocationToTaskSpecificDir(task)
                }
            }
        }
    }

    static File getTaskSpecificSchemaDir(Task task) {
        def schemaBaseDir = task.project.layout.buildDirectory.dir("roomSchemas").get().asFile
        return new File(schemaBaseDir, task.name)
    }

    static def getCompilerPluginOptions(Task task) {
        def processorOptionsField = task.class.superclass.getDeclaredField("processorOptions")
        processorOptionsField.setAccessible(true)
        def compilerPluginOptions = processorOptionsField.get(task)
        return compilerPluginOptions.subpluginOptionsByPluginId[KAPT_SUBPLUGIN_ID]
    }

    private void configureSchemaLocationOutputs(Task task) {
        def processorOptions = getCompilerPluginOptions(task)
        processorOptions.each { option ->
            if (option.key == ROOM_SCHEMA_LOCATION) {
                def relativePath = option.value
                def schemaDestinationDir = task.project.file(relativePath)
                def taskSpecificSchemaDir = getTaskSpecificSchemaDir(task)

                // Add the task specific directory as an output
                task.outputs.dir(taskSpecificSchemaDir)

                // Register the generated schemas to be merged back to the original specified schema directory
                task.project.roomSchemaMergeLocations.registerMerge(schemaDestinationDir, taskSpecificSchemaDir)
            }
        }
    }

    private void setKaptRoomSchemaLocationToTaskSpecificDir(Task task) {
        def processorOptions = getCompilerPluginOptions(task)
        processorOptions.each { option ->
            if (option.key == ROOM_SCHEMA_LOCATION) {
                def taskSpecificSchemaDir = getTaskSpecificSchemaDir(task)

                def valueField = option.class.getDeclaredField("lazyValue")
                valueField.setAccessible(true)
                valueField.set(option, new InitializedLazyImpl(taskSpecificSchemaDir.absolutePath))
            }
        }
    }

    static Class<?> getKaptWithoutKotlincTaskClass() {
        return Class.forName("org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask")
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

    static class MergeAssociations {
        ObjectFactory objectFactory
        Map<File, ConfigurableFileCollection> mergeAssociations = [:]

        MergeAssociations(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory
        }

        void registerMerge(File destination, File source) {
            if (!mergeAssociations.containsKey(destination)) {
                mergeAssociations.put(destination, objectFactory.fileCollection())
            }

            mergeAssociations.get(destination).from(source)
        }
    }

    /**
     * This task is intentionally not incremental.  The intention here is to duplicate the behavior without
     * applying the workaround, which is to only write whatever schemas that were generated during this execution,
     * even if they are incomplete (they really shouldn't be, though).
     *
     * We don't want to create task dependencies on the compile/kapt tasks because we don't want to force execution
     * of those tasks if only a single variant is being assembled.
     */
    static class RoomSchemaLocationMergeTask extends DefaultTask {
        MergeAssociations roomSchemaMergeLocations

        @TaskAction
        void mergeSourcesToDestinations() {
            roomSchemaMergeLocations.mergeAssociations.each { destination, source ->
                project.delete(destination)
                println "Merging schemas to ${destination}"
                project.copy {
                    into(destination)
                    from(source)
                }
            }
        }
    }
}
