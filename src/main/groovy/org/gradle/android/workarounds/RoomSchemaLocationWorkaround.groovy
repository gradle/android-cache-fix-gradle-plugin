package org.gradle.android.workarounds


import org.gradle.android.AndroidIssue
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.util.VersionNumber

import javax.inject.Inject
import java.lang.reflect.Field

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
 *         schemaLocationDir = file("roomSchemas")
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
    public static final String ROOM_SCHEMA_LOCATION = "room.schemaLocation"
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

        // Project extension to hold all of the Room configuration
        def roomExtension = project.extensions.create("room", RoomExtension)

        // Create a task that will be used to merge the task-specific schema locations to the directory (or directories)
        // originally specified.  This allows us to fan out the generated output and keep good cacheability for the
        // compile/kapt tasks but still join everything later in the location the user expects.
        TaskProvider<RoomSchemaLocationMergeTask> mergeTask = project.tasks.register("mergeRoomSchemaLocations", RoomSchemaLocationMergeTask) {
            roomSchemaMergeLocations = roomExtension.roomSchemaMergeLocations
        }

        // Make sure that the annotation processor argument has not been explicitly configured in the Android
        // configuration (i.e. we only want this configured through the room extension
        def configureVariant = { variant ->
            Map<String, String> arguments = variant.javaCompileOptions.annotationProcessorOptions.arguments
            if (arguments.containsKey(ROOM_SCHEMA_LOCATION)) {
                throw new IllegalStateException("""${this.class.name} cannot be used with an explicit '${ROOM_SCHEMA_LOCATION}' annotation processor argument.  Please change this to configure the schema location directory via the 'room' project extension:
    room {
        schemaLocationDir = file("roomSchemas")
    }
""")
            }
        }

        applyToAllAndroidVariants(project, configureVariant)

        project.tasks.withType(JavaCompile).configureEach { JavaCompile task ->
            def taskSpecificSchemaDir = project.objects.directoryProperty()
            taskSpecificSchemaDir.set(getTaskSpecificSchemaDir(task))

            // Add a command line argument provider to the task-specific list of providers
            task.options.compilerArgumentProviders.add(
                new JavaCompilerRoomSchemaLocationArgumentProvider(taskSpecificSchemaDir)
            )

            // Register the generated schemas to be merged back to the original specified schema directory
            roomExtension.registerOutputDirectory(taskSpecificSchemaDir)

            // Seed the task-specific generated schema dir with the existing schemas
            task.doFirst {
                copyExistingSchemasToTaskSpecificTmpDir(task, roomExtension.schemaLocationDir, taskSpecificSchemaDir)
            }

            task.finalizedBy { roomExtension.schemaLocationDir.isPresent() ? mergeTask : null }
        }

        project.plugins.withId("kotlin-kapt") {
            // The kapt task has a list of annotation processor providers which _is_ the list of providers
            // in the Android variant, so we can't just add a task-specific provider.  To handle kapt tasks,
            // we _have_ to add the task-specific provider to the variant.
            applyToAllAndroidVariants(project) { variant ->
                def variantSpecificSchemaDir = project.objects.directoryProperty()
                variantSpecificSchemaDir.set(getVariantSpecificSchemaDir(project, "kapt${variant.name.capitalize()}Kotlin"))
                variant.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(new KaptRoomSchemaLocationArgumentProvider(variantSpecificSchemaDir))

                // Register the variant-specific directory with the merge task
                roomExtension.registerOutputDirectory(variantSpecificSchemaDir)
            }

            // Kapt tasks will remove the contents of any output directories, which will interfere with any additional
            // annotation processors that use the room schema location processor argument and expect existing schemas to
            // be present.  Sooo, we need to generate the schemas to a temporary directory via the annotation processor,
            // then copy the generated schemas to the registered output directory as a last step.  Perhaps this act of
            // pre-seeding the directory with existing schemas should be a capability of the room annotation processor
            // somehow?
            def configureKaptTask = { Task task ->
                task.doFirst {
                    // Populate the variant-specific schemas dir with the existing schemas
                    copyExistingSchemasToTaskSpecificTmpDirForKapt(task, roomExtension.schemaLocationDir)
                }

                task.doLast {
                    // Copy the generated schemas into the registered output directory
                    copyGeneratedSchemasToOutputDirForKapt(task)
                }

                task.finalizedBy { roomExtension.schemaLocationDir.isPresent() ? mergeTask : null }
            }

            project.tasks.withType(kaptWithoutKotlincTaskClass).configureEach(configureKaptTask)
            project.tasks.withType(kaptWithKotlincTaskClass).configureEach(configureKaptTask)

            // Since we've added a new kapt-specific provider to the variant, go through the
            // JavaCompile tasks and remove this provider from its task-specific list so that
            // it only has its JavaCompile-specific provider.  This is not great, but there
            // does not seem to be a way around this with the way the kotlin android plugin
            // maps annotation processor providers from the variant directly onto kapt tasks.
            project.afterEvaluate {
                project.tasks.withType(JavaCompile).configureEach { JavaCompile task ->
                    def itr = task.options.compilerArgumentProviders.iterator()
                    while (itr.hasNext()) {
                        def provider = itr.next()
                        if (provider instanceof KaptRoomSchemaLocationArgumentProvider) {
                            itr.remove()
                        }
                    }
                }
            }
        }
    }

    private static KaptRoomSchemaLocationArgumentProvider getKaptRoomSchemaLocationArgumentProvider(Task task) {
        def annotationProcessorOptionProviders = getAccessibleField(task.class, "annotationProcessorOptionProviders").get(task)
        return annotationProcessorOptionProviders.flatten().find { it instanceof KaptRoomSchemaLocationArgumentProvider }
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

    private static void copyExistingSchemasToTaskSpecificTmpDir(Task task, Provider<Directory> existingSchemaDir, Provider<Directory> taskSpecificTmpDir) {
        // populate the task-specific tmp dir with any existing (non-generated) schemas
        // this allows other annotation processors that might operate on these schemas
        // to find them via the schema location argument
        if (existingSchemaDir.isPresent()) {
            task.project.sync {
                from existingSchemaDir
                into taskSpecificTmpDir
            }
        }
    }

    private static void copyExistingSchemasToTaskSpecificTmpDirForKapt(Task task, Provider<Directory> existingSchemaDir) {
        // Derive the variant directory from the command line provider it is configured with
        def provider = getKaptRoomSchemaLocationArgumentProvider(task)
        if (provider == null) {
            return
        }
        def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir

        // Populate the variant-specific temporary schema dir with the existing schemas
        copyExistingSchemasToTaskSpecificTmpDir(task, existingSchemaDir, temporaryVariantSpecificSchemaDir)
    }

    private static void copyGeneratedSchemasToOutputDirForKapt(Task task) {
        // Copy the generated generated schemas from the task-specific tmp dir to the
        // task-specific output dir.  This dance prevents the kapt task from clearing out
        // the existing schemas before the annotation processors run
        // Derive the variant directory from the command line provider it is configured with
        def provider = getKaptRoomSchemaLocationArgumentProvider(task)
        if (provider == null) {
            return
        }
        def variantSpecificSchemaDir = provider.schemaLocationDir
        def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir

        task.project.sync {
            from temporaryVariantSpecificSchemaDir
            into variantSpecificSchemaDir
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
        @OutputDirectory
        Provider<Directory> schemaLocationDir

        RoomSchemaLocationArgumentProvider(Provider<Directory> schemaLocationDir) {
            this.schemaLocationDir = schemaLocationDir
        }

        @Internal
        protected String getSchemaLocationPath() {
            return schemaLocationDir.get().asFile.absolutePath
        }

        @Override
        Iterable<String> asArguments() {
            if (schemaLocationDir.isPresent()) {
                return ["-A${ROOM_SCHEMA_LOCATION}=${schemaLocationPath}" as String]
            } else {
                return []
            }
        }
    }

    static class JavaCompilerRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
        JavaCompilerRoomSchemaLocationArgumentProvider(Provider<Directory> schemaLocationDir) {
            super(schemaLocationDir)
        }
    }

    static class KaptRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
        private Provider<Directory> temporarySchemaLocationDir

        KaptRoomSchemaLocationArgumentProvider(Provider<Directory> schemaLocationDir) {
            super(schemaLocationDir)
            this.temporarySchemaLocationDir = schemaLocationDir.map {it.dir("../${it.asFile.name}Temp") }
        }

        @Override
        protected String getSchemaLocationPath() {
            return temporarySchemaLocationDir.get().asFile.absolutePath
        }
    }

    static class MergeAssociations {
        ObjectFactory objectFactory
        Map<Provider<Directory>, ConfigurableFileCollection> mergeAssociations = [:]

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
    static class RoomSchemaLocationMergeTask extends DefaultTask {
        @Internal
        MergeAssociations roomSchemaMergeLocations

        @TaskAction
        void mergeSourcesToDestinations() {
            roomSchemaMergeLocations.mergeAssociations.each { destination, source ->
                println "Merging schemas to ${destination.get().asFile}"
                project.copy {
                    duplicatesStrategy(DuplicatesStrategy.INCLUDE)
                    into(destination)
                    from(source)
                }
            }
        }
    }
}
