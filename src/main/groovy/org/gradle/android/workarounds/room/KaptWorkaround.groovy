package org.gradle.android.workarounds.room

import org.gradle.android.VersionNumber
import org.gradle.android.workarounds.room.androidvariants.ConfigureVariants
import org.gradle.android.workarounds.room.argumentprovider.KaptRoomSchemaLocationArgumentProvider
import org.gradle.android.workarounds.room.task.RoomSchemaLocationMergeTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

import java.lang.reflect.Field

// Kapt tasks will remove the contents of any output directories, which will interfere with any additional
// annotation processors that use the room schema location processor argument and expect existing schemas to
// be present.  Sooo, we need to generate the schemas to a temporary directory via the annotation processor,
// then copy the generated schemas to the registered output directory as a last step.  Perhaps this act of
// pre-seeding the directory with existing schemas should be a capability of the room annotation processor
// somehow?
class KaptWorkaround extends AnnotationProcessorWorkaround<KaptRoomSchemaLocationArgumentProvider> {

    KaptWorkaround(Project project, RoomExtension extension, TaskProvider<RoomSchemaLocationMergeTask> mergeTask) {
        super(project, extension, mergeTask)
    }

    static KaptWorkaround create(Project project, RoomExtension extension, TaskProvider<RoomSchemaLocationMergeTask> mergeTask) {
        return new KaptWorkaround(project, extension, mergeTask)
    }

    @Override
    void initWorkaround() {
        // The kapt task has a list of annotation processor providers which _is_ the list of providers
        // in the Android variant, so we can't just add a task-specific provider.  To handle kapt tasks,
        // we _have_ to add the task-specific provider to the variant.
        androidVariantProvider.applyToAllAndroidVariants(project, new ConfigureVariants() {
            @Override
            Closure<?> getOldVariantConfiguration() {
                return { variant ->
                    def variantSpecificSchemaDir = project.objects.directoryProperty()
                    variantSpecificSchemaDir.set(androidVariantProvider.getVariantSpecificSchemaDir(project, "kapt${variant.name.capitalize()}Kotlin"))
                    variant.javaCompileOptions.annotationProcessorOptions.compilerArgumentProviders.add(new KaptRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, variantSpecificSchemaDir))
                }
            }

            @Override
            Closure<?> getNewVariantConfiguration() {
                return { variant ->
                    def variantSpecificSchemaDir = project.objects.directoryProperty()
                    variantSpecificSchemaDir.set(androidVariantProvider.getVariantSpecificSchemaDir(project, "kapt${variant.name.capitalize()}Kotlin"))
                    variant.javaCompilation.annotationProcessor.argumentProviders.add(new KaptRoomSchemaLocationArgumentProvider(roomExtension.schemaLocationDir, variantSpecificSchemaDir))
                }
            }
        })

        project.tasks.withType(kaptWithoutKotlincTaskClass).configureEach {
            configureWorkaroundTask(it)
        }

        // Task KaptWithKotlincTask was removed in 1.8 because Kapt is always run via Gradle workers.
        // https://github.com/JetBrains/kotlin/commit/b8b0b279ee2195ccbdce61e2365f123ee928532b
        if (KotlinVersion.get() < VersionNumber.parse("1.8.0")) {
            project.tasks.withType(kaptWithKotlincTaskClass).configureEach {
                configureWorkaroundTask(it)
            }
        }
    }

    @Override
    void configureWorkaroundTask(Task task) {
        def annotationProcessorOptionProviders = getAccessibleField(task.class, "annotationProcessorOptionProviders").get(task)

        task.doFirst onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
            copyExistingSchemasToTaskSpecificTmpDir(roomExtension.schemaLocationDir, provider)
        }

        task.doLast onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
            copyGeneratedSchemasToOutput(provider)
        }

        task.finalizedBy onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { roomExtension.schemaLocationDir.isPresent() ? mergeTask : null }

        TaskExecutionGraph taskGraph = task.project.gradle.taskGraph
        taskGraph.whenReady onlyIfAnnotationProcessorConfiguredForKapt(annotationProcessorOptionProviders) { KaptRoomSchemaLocationArgumentProvider provider ->
            if (taskGraph.hasTask(task)) {
                roomExtension.registerOutputDirectory(provider.schemaLocationDir)
            }
        }
    }

    @Override
    void copyExistingSchemasToTaskSpecificTmpDir(Provider<Directory> existingSchemaDir, KaptRoomSchemaLocationArgumentProvider provider) {
        if (existingSchemaDir.isPresent()) {
            def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir
            project.fileOperations.sync {
                it.from existingSchemaDir
                it.into temporaryVariantSpecificSchemaDir
            }
        }
    }

    @Override
    void copyGeneratedSchemasToOutput(KaptRoomSchemaLocationArgumentProvider provider) {
        def variantSpecificSchemaDir = provider.schemaLocationDir
        def temporaryVariantSpecificSchemaDir = provider.temporarySchemaLocationDir
        project.fileOperations.sync {
            it.from temporaryVariantSpecificSchemaDir
            it.into variantSpecificSchemaDir
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

    private static Class<?> getKaptWithoutKotlincTaskClass() {
        return Class.forName("org.jetbrains.kotlin.gradle.internal.KaptWithoutKotlincTask")
    }

    private static Class<?> getKaptWithKotlincTaskClass() {
        return Class.forName("org.jetbrains.kotlin.gradle.internal.KaptWithKotlincTask")
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
}
