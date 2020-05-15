package org.gradle.android.workarounds

import kotlin.InitializedLazyImpl
import org.gradle.android.AndroidIssue
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.util.VersionNumber

import java.lang.reflect.Field

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
 * aforementioned merge task.  Note that this workaround only works with Kotlin Gradle plugin 1.3.70 or
 * higher.
 *
 * There are multiple issues related to these problems:
 *  - https://issuetracker.google.com/issues/132245929
 *  - https://issuetracker.google.com/issues/139438151
 */
@AndroidIssue(introducedIn = "3.5.0", fixedIn = [], link = "https://issuetracker.google.com/issues/132245929")
class RoomSchemaLocationWorkaround implements Workaround {
    private static final String ROOM_SCHEMA_LOCATION = "room.schemaLocation"
    private static final String KAPT_SUBPLUGIN_ID = "org.jetbrains.kotlin.kapt3"
    private static final VersionNumber MINIMUM_KOTLIN_VERSION = VersionNumber.parse("1.3.70")
    private static final VersionNumber KOTLIN_VERSION = getKotlinVersion()

    @Override
    boolean canBeApplied(Project project) {
        if (KOTLIN_VERSION != VersionNumber.UNKNOWN && KOTLIN_VERSION < MINIMUM_KOTLIN_VERSION) {
            project.logger.info("${this.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.3.70 or higher (found ${KOTLIN_VERSION.toString()}).")
            return false
        } else {
            return true
        }
    }

    @Override
    void apply(WorkaroundContext context) {
        def project = context.project

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

                // Seed the task-specific generated schema dir with the existing schemas
                task.doFirst {
                    copyExistingSchemasToTaskSpecificTmpDir(task, schemaDestinationDir, taskSpecificSchemaDir)
                }
            }
        )

        // Change the room schema location back to an absolute path right before the kapt tasks execute.
        // This allows other annotation processors that rely on the path being absolute to still function and
        // makes it resilient when the working directory is something other than the project directory.
        // See https://issuetracker.google.com/issues/139438151
        project.plugins.withId("kotlin-kapt") {
            project.tasks.withType(kaptWithoutKotlincTaskClass).configureEach { Task task ->
                task.finalizedBy mergeTask

                project.gradle.taskGraph.beforeTask {
                    if (it == task) {
                        def relativePath = getConfiguredSchemaLocationForKaptWithoutKotlinc(task)
                        if (relativePath) {
                            configureSchemaLocationOutputs(task, relativePath)
                        }
                    }
                }

                doFirst {
                    // Setup a task-specific tmp dir and populate it with the existing schemas
                    setRoomSchemaLocationToTaskSpecificTmpDirForKaptWithoutKotlinc(task)
                }
                doLast {
                    // Copy the generated schemas from the tmp dir to the tracked output dir
                    copyGeneratedSchemasToOutputDir(task, getTaskSpecificSchemaTmpDir(task), getTaskSpecificSchemaDir(task))
                }
            }

            project.tasks.withType(kaptWithKotlincTaskClass).configureEach { Task task ->
                task.finalizedBy mergeTask

                project.gradle.taskGraph.beforeTask {
                    if (it == task) {
                        def relativePath = getConfiguredSchemaLocationOutputForKaptWithKotlinc(task)
                        if (relativePath) {
                            configureSchemaLocationOutputs(task, relativePath)
                        }
                    }
                }

                doFirst {
                    // Setup a task-specific tmp dir and populate it with the existing schemas
                    setRoomSchemaLocationToTaskSpecificTmpDirForKaptWithKotlinc(task)
                }
                doLast {
                    // Copy the generated schemas from the tmp dir to the tracked output dir
                    copyGeneratedSchemasToOutputDir(task, getTaskSpecificSchemaTmpDir(task), getTaskSpecificSchemaDir(task))
                }
            }
        }
    }

    static File getTaskSpecificSchemaDir(Task task) {
        def schemaBaseDir = task.project.layout.buildDirectory.dir("roomSchemas").get().asFile
        return new File(schemaBaseDir, task.name)
    }

    static File getTaskSpecificSchemaTmpDir(Task task) {
        def schemaBaseDir = task.project.layout.buildDirectory.dir("roomSchemas/tmp").get().asFile
        return new File(schemaBaseDir, task.name)
    }

    static def getCompilerPluginOptions(Task task) {
        def processorOptionsField = getAccessibleField(task.class, "processorOptions")
        def compilerPluginOptions = processorOptionsField.get(task)
        return compilerPluginOptions.subpluginOptionsByPluginId[KAPT_SUBPLUGIN_ID]
    }

    static def getEncodedCompilerPluginOptions(Task task) {
        def pluginOptionsField = getAccessibleField(task.class, "pluginOptions")
        def compilerPluginOptions = pluginOptionsField.get(task)
        def optionsList = compilerPluginOptions.subpluginOptionsByPluginId[KAPT_SUBPLUGIN_ID]
        return optionsList.find { it.key == "apoptions" }
    }

    private static Map<String, String> decode(String encoded) {
        Map<String, String> map = [:]
        def ois = new ObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))
        int size = ois.readInt()
        size.times {
            map.put(ois.readUTF(), ois.readUTF())
        }
        return map
    }

    private static String encode(Map<String, String> map) {
        def os = new ByteArrayOutputStream()
        def oos = new ObjectOutputStream(os)

        oos.writeInt(map.size())
        map.each { key, value ->
            oos.writeUTF(key)
            oos.writeUTF(value)
        }

        oos.flush()
        return Base64.getEncoder().encodeToString(os.toByteArray())
    }

    private static String getConfiguredSchemaLocationOutputForKaptWithKotlinc(Task task) {
        def apOptions = decode(getEncodedCompilerPluginOptions(task).value)
        return apOptions[ROOM_SCHEMA_LOCATION]
    }

    private static String getConfiguredSchemaLocationForKaptWithoutKotlinc(Task task) {
        def processorOptions = getCompilerPluginOptions(task)
        def option = processorOptions.find { it.key == ROOM_SCHEMA_LOCATION }
        return option?.value
    }

    private static void configureSchemaLocationOutputs(Task task, String relativePath) {
        def schemaDestinationDir = task.project.file(relativePath)
        def taskSpecificSchemaDir = getTaskSpecificSchemaDir(task)

        // Add the task specific directory as an output
        task.outputs.dir(taskSpecificSchemaDir)

        // Register the generated schemas to be merged back to the original specified schema directory
        task.project.roomSchemaMergeLocations.registerMerge(schemaDestinationDir, taskSpecificSchemaDir)
    }

    private static void copyExistingSchemasToTaskSpecificTmpDir(Task task, File existingSchemaDir, File taskSpecificTmpDir) {
        // populate the task-specific tmp dir with any existing (non-generated) schemas
        // this allows other annotation processors that might operate on these schemas
        // to find them via the schema location argument
        task.project.sync {
            from existingSchemaDir
            into taskSpecificTmpDir
        }
    }

    private static void copyGeneratedSchemasToOutputDir(Task task, File taskSpecificTmpDir, File outputDir) {
        // Copy the generated generated schemas from the task-specific tmp dir to the
        // task-specific output dir.  This dance prevents the kapt task from clearing out
        // the existing schemas before the annotation processors run
        task.project.sync {
            from taskSpecificTmpDir
            into outputDir
        }
    }

    private static void setRoomSchemaLocationToTaskSpecificTmpDirForKaptWithoutKotlinc(Task task) {
        def processorOptions = getCompilerPluginOptions(task)
        processorOptions.each { option ->
            if (option.key == ROOM_SCHEMA_LOCATION) {
                def taskSpecificTmpDir = getTaskSpecificSchemaTmpDir(task)
                copyExistingSchemasToTaskSpecificTmpDir(task, task.project.file(option.value), taskSpecificTmpDir)
                setOptionValue(option, taskSpecificTmpDir.absolutePath)
            }
        }
    }

    private static void setRoomSchemaLocationToTaskSpecificTmpDirForKaptWithKotlinc(Task task) {
        def encodedOptions = getEncodedCompilerPluginOptions(task)
        def apOptions = decode(encodedOptions.value)
        if (apOptions.containsKey(ROOM_SCHEMA_LOCATION)) {
            def taskSpecificTmpDir = getTaskSpecificSchemaTmpDir(task)
            copyExistingSchemasToTaskSpecificTmpDir(task, task.project.file(apOptions[ROOM_SCHEMA_LOCATION]), taskSpecificTmpDir)
            apOptions[ROOM_SCHEMA_LOCATION] = taskSpecificTmpDir.absolutePath
            setOptionValue(encodedOptions, encode(apOptions))
        }
    }

    private static void setOptionValue(Object option, String value) {
        def valueField = getAccessibleField(option.class, "lazyValue")
        valueField.set(option, new InitializedLazyImpl(value))
    }

    private static Field getAccessibleField(Class<?> clazz, String fieldName) {
        for (Field field : clazz.declaredFields) {
            if (field.name == fieldName) {
                field.setAccessible(true)
                return field
            }
        }

        if (clazz.superclass != null) {
            return getAccessibleField(clazz.superclass, fieldName)
        } else {
            throw new RuntimeException("Field '${fieldName}' not found")
        }
    }

    static Class<?> getKaptWithoutKotlincTaskClass() {
        return Class.forName("org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask")
    }

    static Class<?> getKaptWithKotlincTaskClass() {
        return Class.forName("org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask")
    }

    static VersionNumber getKotlinVersion() {
        def projectPropertiesStream = RoomSchemaLocationWorkaround.class.classLoader.getResourceAsStream("project.properties")
        if (projectPropertiesStream != null) {
            def projectProperties = new Properties()
            projectProperties.load(projectPropertiesStream)
            if (projectProperties.containsKey("project.version")) {
                return VersionNumber.parse(projectProperties.getProperty("project.version"))
            }
        }

        return VersionNumber.UNKNOWN
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
        @Internal
        MergeAssociations roomSchemaMergeLocations

        @TaskAction
        void mergeSourcesToDestinations() {
            roomSchemaMergeLocations.mergeAssociations.each { destination, source ->
                println "Merging schemas to ${destination}"
                project.copy {
                    duplicatesStrategy(DuplicatesStrategy.INCLUDE)
                    into(destination)
                    from(source)
                }
            }
        }
    }
}
