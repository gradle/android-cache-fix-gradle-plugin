package org.gradle.android.workarounds

import org.gradle.android.AndroidIssue
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations

import javax.inject.Inject
import java.lang.module.ModuleDescriptor
import java.nio.ByteBuffer
import java.nio.file.Files

@AndroidIssue(introducedIn = "7.1.0", fixedIn = [], link = "")
class JdkImageWorkaround implements Workaround {
    static final String JDK_IMAGE = "_internal_android_jdk_image"
    static final String JDK_IMAGE_EXTRACTED = "_internal_android_jdk_image_extracted"
    static final String JDK_IMAGE_CONFIG_NAME = "androidJdkImage"

    @Override
    void apply(Project project) {
        applyToAllAndroidVariants(project) { variant ->
            variant.javaCompileProvider.configure { JavaCompile task ->
                def jdkImageInput = getJdkImageInput(task)
                if (jdkImageInput != null) {
                    setupExtractedJdkImageInputTransform(project, task.getJavaCompiler().map { it.metadata.installationPath })
                    replaceCommandLineProvider(task, jdkImageInput)
                    applyRuntimeClasspathNormalization(task.project)
                }
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

    static def applyRuntimeClasspathNormalization(Project project) {
        project.normalization { handler ->
            handler.runtimeClasspath {
                it.metaInf { metaInfNormalization ->
                    metaInfNormalization.ignoreAttribute('Implementation-Version')
                    metaInfNormalization.ignoreAttribute('Implementation-Vendor')
                    metaInfNormalization.ignoreAttribute('Created-By')
                }
            }
        }
    }

    static def getJdkImageInput(JavaCompile task) {
        return task.options.compilerArgumentProviders.find { it.class.simpleName == "JdkImageInput" }
    }

    static void setupExtractedJdkImageInputTransform(Project project, Provider<Directory> javaHome) {
        project.dependencies.registerTransform(ExtractJdkImageTransform) {spec ->
            spec.from.attribute(ArtifactAttributes.ARTIFACT_FORMAT, JDK_IMAGE)
            spec.to.attribute(ArtifactAttributes.ARTIFACT_FORMAT, JDK_IMAGE_EXTRACTED)
            spec.parameters.javaHome = javaHome
        }
    }

    static void replaceCommandLineProvider(JavaCompile task, jdkImageInput) {
        Attribute<String> jdkId = Attribute.of("jdk-id", String)
        def jdkConfiguration = task.project.configurations.getByName(JDK_IMAGE_CONFIG_NAME)
        def extractedJdkImage = jdkConfiguration.incoming.artifactView {viewConfiguration ->
            viewConfiguration.attributes {
                it.attribute(ArtifactAttributes.ARTIFACT_FORMAT, JDK_IMAGE_EXTRACTED)
                it.attribute(jdkId, task.javaCompiler.get().metadata.jvmVersion + task.javaCompiler.get().metadata.vendor)
            }
        }.artifacts.artifactFiles
        def extractedJdkImageProvider = new ExtractedJdkImageCommandLineProvider(extractedJdkImage, jdkImageInput.jdkImage)
        task.options.compilerArgumentProviders.remove(jdkImageInput)
        task.options.compilerArgumentProviders.add(extractedJdkImageProvider)
    }

    @Override
    boolean canBeApplied(Project project) {
        return true
    }

    static class ExtractedJdkImageCommandLineProvider implements CommandLineArgumentProvider {
        @Classpath
        final FileCollection extractedJdkImage

        @Internal
        final FileCollection jdkImage

        ExtractedJdkImageCommandLineProvider(FileCollection extractedJdkImage, FileCollection jdkImage) {
            this.extractedJdkImage = extractedJdkImage
            this.jdkImage = jdkImage
        }

        @Override
        Iterable<String> asArguments() {
            return ["--system", new File(jdkImage.singleFile, "jdkImage").absolutePath]
        }
    }

    abstract static class ExtractJdkImageTransform implements TransformAction<Parameters> {
        interface Parameters extends TransformParameters {
            @Internal
            Provider<Directory> getJavaHome()
            void setJavaHome(Provider<Directory> javaHome)
        }

        @Inject
        abstract ExecOperations getExecOperations()

        @Inject
        abstract FileSystemOperations getFileOperations()

        @Classpath
        @InputArtifact
        abstract Provider<FileSystemLocation> getJdkImageDir()

        @Override
        void transform(TransformOutputs outputs) {
            // Extract the contents of the runtime jimage file
            def targetDir = outputs.dir("extracted")
            execOperations.exec {
                executable = new File(parameters.javaHome.get().asFile, "bin/jimage")
                args(
                    "extract",
                    "--dir",
                    targetDir,
                    new File(jdkImageDir.get().asFile, "jdkImage/lib/modules").absolutePath
                )
            }

            // Capture the module descriptor ignoring the version, which is not enforced anyways
            File moduleInfoFile = new File(targetDir, 'java.base/module-info.class')
            ModuleDescriptor strippedDescriptor = captureModuleDescriptorWithoutVersion(moduleInfoFile)
            File descriptorData = new File(targetDir, "module-descriptor.txt")
            descriptorData.text = strippedDescriptor.toString()

            fileOperations.delete {
                delete(moduleInfoFile)
            }
        }

        private static ModuleDescriptor captureModuleDescriptorWithoutVersion(File moduleFile) {
            ModuleDescriptor descriptor = ModuleDescriptor.read(ByteBuffer.wrap(Files.readAllBytes(moduleFile.toPath())))
            ModuleDescriptor.Builder strippedDescriptor = ModuleDescriptor.newModule(descriptor.name())
            strippedDescriptor.packages(descriptor.packages())
            if (descriptor.mainClass().present) {
                strippedDescriptor.mainClass(descriptor.mainClass().get())
            }
            descriptor.exports().each { strippedDescriptor.exports(it) }
            descriptor.opens().each {strippedDescriptor.opens(it) }
            descriptor.provides().each { strippedDescriptor.provides(it) }
            descriptor.requires().each { strippedDescriptor.requires(it) }
            descriptor.uses()each { strippedDescriptor.uses(it) }
            return strippedDescriptor.build()
        }
    }
}
