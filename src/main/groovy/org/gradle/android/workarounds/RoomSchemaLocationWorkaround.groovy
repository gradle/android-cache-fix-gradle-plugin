package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.android.VersionNumber
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject
import java.lang.reflect.Field
import java.util.function.Supplier

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
    private static final VersionNumber MINIMUM_KOTLIN_VERSION = VersionNumber.parse("1.4.32")
    private static final VersionNumber KOTLIN_VERSION = getKotlinVersion()

    @Override
    boolean canBeApplied(Project project) {
        if (KOTLIN_VERSION != VersionNumber.UNKNOWN && KOTLIN_VERSION < MINIMUM_KOTLIN_VERSION) {
            project.logger.info("${this.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.4.32 or higher (found ${KOTLIN_VERSION.toString()}).")
            return false
        } else {
            return SystemPropertiesCompat.getBoolean(WORKAROUND_ENABLED_PROPERTY, project, true)
        }
    }

    @Override
    void apply(Project project) {
        // Project extension to hold all of the Room configuration
        def roomExtension = project.extensions.create("room", RoomExtension)

        // Grab fileOperations so we can do copy/sync operations
        def fileOperations = project.fileOperations

        // An undefined directory property for use when providers are disabled
        def nullDirectory = project.objects.directoryProperty()

        // Create a task that will be used to merge the task-specific schema locations to the directory (or directories)
        // originally specified.  This allows us to fan out the generated output and keep good cacheability for the
        // compile/kapt tasks but still join everything later in the location the user expects.
        TaskProvider<RoomSchemaLocationMergeTask> mergeTask = project.tasks.register("mergeRoomSchemaLocations", RoomSchemaLocationMergeTask) {
            roomSchemaMergeLocations = roomExtension.roomSchemaMergeLocations
        }

        boolean javaCompileSchemaGenerationEnabled = true

        def configureVariant = { variant ->
            // Make sure that the annotation processor argument has not been explicitly configured in the Android
            // configuration (i.e. we only want this configured through the room extension
            Map<String, String> arguments = variant.javaCompileOptions.annotationProcessorOptions.arguments
            if (arguments.containsKey(ROOM_SCHEMA_LOCATION)) {
                throw new IllegalStateException("""${this.class.name} cannot be used with an explicit '${ROOM_SCHEMA_LOCATION}' annotation processor argument.  Please change this to configure the schema location directory via the 'room' project extension:
    room {
        schemaLocationDir.set(file("roomSchemas"))
    }
""")
            }

            // Configure the annotation processor argument provider on the Java compile task
            javaCompileProvider.configure { JavaCompile task ->
                def taskSpecificSchemaDir = project.objects.directoryProperty()
                taskSpecificSchemaDir.set(getTaskSpecificSchemaDir(task))

                // Add a command line argument provider to the task-specific list of providers
                task.options.compilerArgumentProviders.add(
                    new JavaCompilerRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, taskSpecificSchemaDir, nullDirectory, { javaCompileSchemaGenerationEnabled } as Supplier)
                )

                // Register the generated schemas to be merged back to the original specified schema directory
                task.project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
                    if (graph.hasTask(task)) {
                        roomExtension.registerOutputDirectory(taskSpecificSchemaDir)
                    }
                }

                // Seed the task-specific generated schema dir with the existing schemas
                task.doFirst {
                    if (javaCompileSchemaGenerationEnabled) {
                        RoomSchemaLocationWorkaround.copyExistingSchemasToTaskSpecificTmpDir(fileOperations, roomExtension.schemaLocationDir, taskSpecificSchemaDir)
                    }
                }

                task.finalizedBy { roomExtension.schemaLocationDir.isPresent() ? mergeTask : null }
            }
        }

        applyToAllAndroidVariants(project, configureVariant)

        project.plugins.withId("kotlin-kapt") {
            // The kapt task has a list of annotation processor providers which _is_ the list of providers
            // in the Android variant, so we can't just add a task-specific provider.  To handle kapt tasks,
            // we _have_ to add the task-specific provider to the variant.
            applyToAllAndroidVariants(project) { variant ->
                def variantSpecificSchemaDir = project.objects.directoryProperty()
                variantSpecificSchemaDir.set(getVariantSpecificSchemaDir(project, "kapt${variant.name.capitalize()}Kotlin"))
                variant.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(new KaptRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, variantSpecificSchemaDir, nullDirectory))
            }

            // Kapt tasks will remove the contents of any output directories, which will interfere with any additional
            // annotation processors that use the room schema location processor argument and expect existing schemas to
            // be present.  Sooo, we need to generate the schemas to a temporary directory via the annotation processor,
            // then copy the generated schemas to the registered output directory as a last step.  Perhaps this act of
            // pre-seeding the directory with existing schemas should be a capability of the room annotation processor
            // somehow?
            def configureKaptTask = { Task task ->
                def annotationProcessorOptionProviders = getAccessibleField(task.class, "annotationProcessorOptionProviders").get(task)

                task.doFirst onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
                    // Populate the variant-specific schemas dir with the existing schemas
                    RoomSchemaLocationWorkaround.copyExistingSchemasToTaskSpecificTmpDirForKapt(fileOperations, roomExtension.schemaLocationDir, provider)
                }

                task.doLast onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
                    // Copy the generated schemas into the registered output directory
                    RoomSchemaLocationWorkaround.copyGeneratedSchemasToOutputDirForKapt(fileOperations, provider)
                }

                task.finalizedBy onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { roomExtension.schemaLocationDir.isPresent() ? mergeTask : null }

                TaskExecutionGraph taskGraph = task.project.gradle.taskGraph
                taskGraph.whenReady onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
                    if (taskGraph.hasTask(task)) {
                        // Register the variant-specific directory with the merge task
                        roomExtension.registerOutputDirectory(provider.schemaLocationDir)
                    }
                }
            }

            project.tasks.withType(kaptWithoutKotlincTaskClass).configureEach(configureKaptTask)
            project.tasks.withType(kaptWithKotlincTaskClass).configureEach(configureKaptTask)

            // Since we've added a new kapt-specific provider to the variant, disable the provider
            // used for the JavaCompile task.  This is not great, but there
            // does not seem to be a way around this with the way the kotlin android plugin
            // maps annotation processor providers from the variant directly onto kapt tasks.
            javaCompileSchemaGenerationEnabled = false
        }
    }

    private static Closure onlyIfAnnotationProcessorConfiguredForKapt(def annotationProcessorOptionProviders, Closure<?> action) {
        return {
            def provider = annotationProcessorOptionProviders.flatten().find { it instanceof KaptRoomSchemaLocationArgumentProvider }
            if (provider != null) {
                action.call(provider)
            }
        }
    }

    private static void applyToAllAndroidVariants(Project project, Closure<?> configureVariant) {
        project.plugins.withId("com.android.application") {
            def android = project.extensions.findByName("android")

            android.applicationVariants.all(configureVariant)
        }

        project.plugins.withId("com.android.library") {
            def android = project.extensions.findByName("android")

            android.libraryVariants.all(configureVariant)
        }
    }

    static File getTaskSpecificSchemaDir(Task task) {
        def schemaBaseDir = task.project.layout.buildDirectory.dir("roomSchemas").get().asFile
        return new File(schemaBaseDir, task.name)
    }

    static File getVariantSpecificSchemaDir(Project project, String variantName) {
        def schemaBaseDir = project.layout.buildDirectory.dir("roomSchemas").get().asFile
        return new File(schemaBaseDir, variantName)
    }

    private static void copyExistingSchemasToTaskSpecificTmpDir(FileOperations fileOperations, Provider<Directory> existingSchemaDir, Provider<Directory> taskSpecificTmpDir) {
        // populate the task-specific tmp dir with any existing (non-generated) schemas
        // this allows other annotation processors that might operate on these schemas
        // to find them via the schema location argument
        if (existingSchemaDir.isPresent()) {

            fileOperations.sync {
                it.from(existingSchemaDir)
                it.into(taskSpecificTmpDir)
            }
        }
    }

    private static void copyExistingSchemasToTaskSpecificTmpDirForKapt(FileOperations fileOperations, Provider<Directory> existingSchemaDir, KaptRoomSchemaLocationArgumentProvider provider) {
        // Derive the variant directory from the command line provider it is configured with
        def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir

        // Populate the variant-specific temporary schema dir with the existing schemas
        copyExistingSchemasToTaskSpecificTmpDir(fileOperations, existingSchemaDir, temporaryVariantSpecificSchemaDir)
    }

    private static void copyGeneratedSchemasToOutputDirForKapt(FileOperations fileOperations, KaptRoomSchemaLocationArgumentProvider provider) {
        // Copy the generated generated schemas from the task-specific tmp dir to the
        // task-specific output dir.  This dance prevents the kapt task from clearing out
        // the existing schemas before the annotation processors run
        // Derive the variant directory from the command line provider it is configured with
        def variantSpecificSchemaDir = provider.schemaLocationDir
        def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir

        fileOperations.sync {
            it.from temporaryVariantSpecificSchemaDir
            it.into variantSpecificSchemaDir
        }
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

    static abstract class RoomExtension {
        DirectoryProperty schemaLocationDir
        MergeAssociations roomSchemaMergeLocations

        @Inject
        RoomExtension(ObjectFactory objectFactory) {
            schemaLocationDir = objectFactory.directoryProperty()
            roomSchemaMergeLocations = objectFactory.newInstance(MergeAssociations)
        }

        void registerOutputDirectory(Provider<Directory> outputDir) {
            roomSchemaMergeLocations.registerMerge(schemaLocationDir, outputDir)
        }
    }

    static abstract class RoomSchemaLocationArgumentProvider implements CommandLineArgumentProvider {
        @Internal
        final Provider<Directory> configuredSchemaLocationDir

        @Internal
        final Provider<Directory> schemaLocationDir

        @Internal
        final Supplier<Boolean> enabled

        @Internal
        final Provider<Directory> nullDirectory

        RoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir, Provider<Directory> nullDirectory, Supplier<Boolean> enabled) {
            this.configuredSchemaLocationDir = configuredSchemaLocationDir
            this.enabled = enabled
            this.schemaLocationDir = schemaLocationDir
            this.nullDirectory = nullDirectory
        }

        @Internal
        protected String getSchemaLocationPath() {
            return schemaLocationDir.get().asFile.absolutePath
        }

        @Override
        Iterable<String> asArguments() {
            if (configuredSchemaLocationDir.isPresent() && enabled.get()) {
                return ["-A${ROOM_SCHEMA_LOCATION}=${schemaLocationPath}" as String]
            } else {
                return []
            }
        }

        @OutputDirectory
        @Optional
        Provider<Directory> getEffectiveSchemaLocationDir() {
            return enabled.get() ? schemaLocationDir : nullDirectory
        }
    }

    static class JavaCompilerRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
        JavaCompilerRoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir, Provider<Directory> nullDirectory, Supplier<Boolean> enabled) {
            super(configuredSchemaLocationDir, schemaLocationDir, nullDirectory, enabled)
        }
    }

    static class KaptRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
        private Provider<Directory> temporarySchemaLocationDir

        KaptRoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir, Provider<Directory> nullDirectory) {
            super(configuredSchemaLocationDir, schemaLocationDir, nullDirectory, { true } as Supplier<Boolean>)
            this.temporarySchemaLocationDir = schemaLocationDir.map {it.dir("../${it.asFile.name}Temp") }
        }

        @Override
        protected String getSchemaLocationPath() {
            return temporarySchemaLocationDir.get().asFile.absolutePath
        }
    }

    static class MergeAssociations {
        final ObjectFactory objectFactory
        final Map<Provider<Directory>, ConfigurableFileCollection> mergeAssociations = [:]

        @Inject
        MergeAssociations(ObjectFactory objectFactory) {
            this.objectFactory = objectFactory
        }

        void registerMerge(Provider<Directory> destination, Provider<Directory> source) {
            if (!mergeAssociations.containsKey(destination)) {
                mergeAssociations.put(destination, objectFactory.fileCollection())
            }

            mergeAssociations.get(destination).from(source)
        }
    }

    /**
     * This task is intentionally not incremental.  The intention here is to duplicate the behavior the user
     * experiences when the workaround is not applied, which is to only write whatever schemas that were generated
     * during this execution, even if they are incomplete (they really shouldn't be, though).
     *
     * We don't want to create task dependencies on the compile/kapt tasks because we don't want to force execution
     * of those tasks if only a single variant is being assembled.
     */
    @DisableCachingByDefault(because = 'This is a disk bound copy/merge task.')
    static abstract class RoomSchemaLocationMergeTask extends DefaultTask {

        // Using older internal API to maintain compatibility with Gradle 5.x
        @Inject abstract FileOperations getFileOperations()

        @Internal
        MergeAssociations roomSchemaMergeLocations

        @TaskAction
        void mergeSourcesToDestinations() {
            roomSchemaMergeLocations.mergeAssociations.each { destination, source ->
                println "Merging schemas to ${destination.get().asFile}"
                fileOperations.copy {
                    it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    it.into(destination)
                    it.from(source)
                }
            }
        }
    }
}
