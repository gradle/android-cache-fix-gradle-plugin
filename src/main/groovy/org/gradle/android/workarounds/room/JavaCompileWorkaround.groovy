package org.gradle.android.workarounds.room

import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.android.workarounds.room.androidvariants.ConfigureVariants
import org.gradle.android.workarounds.room.argumentprovider.JavaCompilerRoomSchemaLocationArgumentProvider
import org.gradle.android.workarounds.room.argumentprovider.RoomSchemaLocationArgumentProvider
import org.gradle.android.workarounds.room.task.RoomSchemaLocationMergeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.Directory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

class JavaCompileWorkaround extends AnnotationProcessorWorkaround<JavaCompilerRoomSchemaLocationArgumentProvider> {

    boolean javaCompileSchemaGenerationEnabled = true

    JavaCompileWorkaround(Project project, RoomExtension roomExtension, TaskProvider<RoomSchemaLocationMergeTask> mergeTask) {
        super(project, roomExtension, mergeTask)
    }

    static JavaCompileWorkaround create(Project project, RoomExtension extension, TaskProvider<RoomSchemaLocationMergeTask> mergeTask) {
        return new JavaCompileWorkaround(project, extension, mergeTask)
    }

    @Override
    void initWorkaround() {


        androidVariantProvider.applyToAllAndroidVariants(project, new ConfigureVariants() {
            @Override
            Closure<?> getOldVariantConfiguration() {
                return { variant ->
                    // Make sure that the annotation processor argument has not been explicitly configured in the Android
                    // configuration (i.e. we only want this configured through the room extension)
                    Map<String, String> arguments = variant.javaCompileOptions.annotationProcessorOptions.arguments
                    errorIfRoomSchemaAnnotationArgumentSet(arguments.keySet())

                    def variantSpecificSchemaDir = project.objects.directoryProperty()
                    variantSpecificSchemaDir.set(androidVariantProvider.getVariantSpecificSchemaDir(project, "compile${variant.name.capitalize()}JavaWithJavac"))

                    // Add a command line argument provider to the compile task argument providers
                    variant.javaCompileProvider.configure { JavaCompile task ->
                        task.options.compilerArgumentProviders.add(
                            new JavaCompilerRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, variantSpecificSchemaDir)
                        )
                    }

                }
            }

            @Override
            Closure<?> getNewVariantConfiguration() {
                return { variant ->
                    // Make sure that the annotation processor argument has not been explicitly configured in the Android
                    // configuration (i.e. we only want this configured through the room extension
                    MapProperty<String, String> arguments = variant.javaCompilation.annotationProcessor.arguments
                    errorIfRoomSchemaAnnotationArgumentSet(arguments.keySet().get())

                    def variantSpecificSchemaDir = project.objects.directoryProperty()
                    variantSpecificSchemaDir.set(androidVariantProvider.getVariantSpecificSchemaDir(project, "compile${variant.name.capitalize()}JavaWithJavac"))

                    variant.javaCompilation.annotationProcessor.argumentProviders.add(
                        new JavaCompilerRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, variantSpecificSchemaDir)
                    )
                }
            }
        })

        project.tasks.withType(JavaCompile).configureEach {
            configureWorkaroundTask(it)
        }
    }

    @Override
    void configureWorkaroundTask(Task task) {
        TaskExecutionGraph taskGraph = task.project.gradle.taskGraph

        taskGraph.whenReady {
            if (taskGraph.hasTask(task)) {
                if (javaCompileSchemaGenerationEnabled) {
                    // Seed the task-specific generated schema dir with the existing schemas
                    task.doFirst onlyIfAnnotationProcessorConfiguredForJavaCompile(task.options.compilerArgumentProviders) { JavaCompilerRoomSchemaLocationArgumentProvider provider ->
                        copyExistingSchemasToTaskSpecificTmpDir(roomExtension.schemaLocationDir, provider)
                    }

                    // Register the generated schemas to be merged back to the original specified schema directory
                    task.configure onlyIfAnnotationProcessorConfiguredForJavaCompile(task.options.compilerArgumentProviders) { JavaCompilerRoomSchemaLocationArgumentProvider provider ->
                        roomExtension.registerOutputDirectory(provider.schemaLocationDir)
                    }
                } else {
                    // If kapt is enabled, then those tasks will do the annotation processing, and we should go through
                    // and remove the provider from the JavaCompile tasks since we don't want their outputs to be considered
                    def itr = task.options.compilerArgumentProviders.iterator()
                    while (itr.hasNext()) {
                        CommandLineArgumentProvider provider = itr.next()
                        if (provider instanceof RoomSchemaLocationArgumentProvider) {
                            itr.remove()
                        }
                    }
                }
            }
        }

        task.finalizedBy {
            onlyIfAnnotationProcessorConfiguredForJavaCompile(task.options.compilerArgumentProviders) {
                roomExtension.schemaLocationDir.isPresent() ? mergeTask : null
            }
        }
    }

    @Override
    void copyExistingSchemasToTaskSpecificTmpDir(Provider<Directory> existingSchemaDir, JavaCompilerRoomSchemaLocationArgumentProvider provider) {
        // Derive the variant directory from the command line provider it is configured with
        def variantSpecificSchemaDir = provider.schemaLocationDir

        // Populate the variant-specific temporary schema dir with the existing schemas
        project.fileOperations.sync {
            it.from existingSchemaDir
            it.into variantSpecificSchemaDir
        }
    }

    @Override
    void copyGeneratedSchemasToOutput(JavaCompilerRoomSchemaLocationArgumentProvider provider) {
    }

    private static Closure onlyIfAnnotationProcessorConfiguredForJavaCompile(def argumentProviders, Closure<?> action) {
        return {
            def provider = argumentProviders.find { it instanceof JavaCompilerRoomSchemaLocationArgumentProvider }
            if (provider != null) {
                action.call(provider)
            }
        }
    }

    static void errorIfRoomSchemaAnnotationArgumentSet(Set<String> options) {
        if (options.contains(RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION)) {
            throw new IllegalStateException("""${RoomSchemaLocationWorkaround.class.name} cannot be used with an explicit '${RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION}' annotation processor argument.  Please change this to configure the schema location directory via the 'room' project extension:
     room {
         schemaLocationDir.set(file("roomSchemas"))
     }
 """)
        }
    }
}
