package org.gradle.android.workarounds.room.androidvariants

import org.gradle.android.VersionNumber
import org.gradle.android.Versions
import org.gradle.api.Project

class ApplyAndroidVariants {

    static File getVariantSpecificSchemaDir(Project project, String variantName) {
        def schemaBaseDir = project.layout.buildDirectory.dir("roomSchemas").get().asFile
        return new File(schemaBaseDir, variantName)
    }

    static void applyToAllAndroidVariants(Project project, ConfigureVariants configureVariants) {
        if (Versions.CURRENT_ANDROID_VERSION <= VersionNumber.parse("7.4.0-alpha01")) {
            applyToAllAndroidVariantsWithOldVariantApi(project, configureVariants)
        } else {
            applyToAllAndroidVariantsWithNewVariantApi(project, configureVariants)
        }
    }

    private static void applyToAllAndroidVariantsWithOldVariantApi(Project project, ConfigureVariants configureVariants) {
        project.plugins.withId("com.android.application") {
            def android = project.extensions.findByName("android")

            android.applicationVariants.all(configureVariants.oldVariantConfiguration)
        }

        project.plugins.withId("com.android.library") {
            def android = project.extensions.findByName("android")

            android.libraryVariants.all(configureVariants.oldVariantConfiguration)
        }
    }

    private static void applyToAllAndroidVariantsWithNewVariantApi(Project project, ConfigureVariants configureVariants) {
        project.plugins.withId("com.android.application") {
            configureNewVariants(project, configureVariants)
        }

        project.plugins.withId("com.android.library") {
            configureNewVariants(project, configureVariants)
        }
    }

    private static configureNewVariants(Project project, ConfigureVariants configureVariants){
        def androidComponents = project.extensions.findByName("androidComponents")
        def selector = androidComponents.selector()
        androidComponents.onVariants(selector.all(), configureVariants.newVariantConfiguration)
    }
}
