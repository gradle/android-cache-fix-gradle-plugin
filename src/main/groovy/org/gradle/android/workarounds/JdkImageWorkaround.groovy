package org.gradle.android.workarounds

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import org.gradle.android.AndroidIssue
import org.gradle.android.VersionNumber
import org.gradle.android.Versions
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.CacheableTransform
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
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Works around cache misses due to the custom Java runtime used when source compatibility is set higher
 * than Java 9.  This normalizes out minor inconsequential differences between JDKs used to generate the
 * custom runtime and improve cache hits between environments.
 */
@AndroidIssue(introducedIn = "7.1.0", link = "https://issuetracker.google.com/u/1/issues/234820480")
class JdkImageWorkaround implements Workaround {
    static final String WORKAROUND_ENABLED_PROPERTY = "org.gradle.android.cache-fix.JdkImageWorkaround.enabled"

    static final String JDK_IMAGE = "_internal_android_jdk_image"
    static final String JDK_IMAGE_EXTRACTED = "_internal_android_jdk_image_extracted"
    static final String JDK_IMAGE_CONFIG_NAME = "androidJdkImage"

    @Override
    void apply(Project project) {
        // We would prefer not to configure this if a jdkImage is not in use, but the attributes
        // being ignored are unlikely to ever have a runtime impact.  Doing this outside of task
        // configuration prevents issues with things that use the tooling api to finalize the
        // runtime configuration before querying (and instantiating) task configurations.
        applyRuntimeClasspathNormalization(project)

        applyToAllAndroidVariants(project) { variant ->
            variant.javaCompileProvider.configure { JavaCompile task ->
                jdkTransform(project, task)
            }
        }
    }

    private static void jdkTransform(Project project, JavaCompile task) {
        def jdkImageInput = getJdkImageInput(task)
        if (jdkImageInput != null) {
            setupExtractedJdkImageInputTransform(project, getJvmHome(task))
            replaceCommandLineProvider(task, jdkImageInput)
        }
    }

    // Configuration for Old Variant API will drop in AGP 9. We will need to use a different
    // approach to retrieve the variants using the new Variant API.
    private static void applyToAllAndroidVariants(Project project, Closure<?> configureVariant) {
        project.plugins.withId("com.android.application") {
            def android = project.extensions.findByName("android")
            android.unitTestVariants.all(configureVariant)
            android.applicationVariants.all(configureVariant)
        }

        project.plugins.withId("com.android.library") {
            def android = project.extensions.findByName("android")
            android.unitTestVariants.all(configureVariant)
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
                it.attribute(jdkId, getJvmVersion(task) + getJvmVendor(task))
            }
        }.artifacts.artifactFiles
        def extractedJdkImageProvider = new ExtractedJdkImageCommandLineProvider(extractedJdkImage, jdkImageInput.jdkImage)
        task.options.compilerArgumentProviders.remove(jdkImageInput)
        task.options.compilerArgumentProviders.add(extractedJdkImageProvider)
    }

    static String getJvmVersion(JavaCompile task) {
        return task.javaCompiler.map { it.metadata.jvmVersion }
            .orElse(SystemPropertiesCompat.getString("java.version", task.project, null))
    }

    static String getJvmVendor(JavaCompile task) {
        return task.javaCompiler.map { it.metadata.vendor }
            .orElse(SystemPropertiesCompat.getString("java.vendor", task.project, null))
    }

    static Provider<Directory> getJvmHome(JavaCompile task) {
        def defaultJvmHome = task.project.objects.directoryProperty()
        defaultJvmHome.set(new File(SystemPropertiesCompat.getString("java.home", task.project, null)))
        return task.javaCompiler.map { it.metadata.installationPath }
            .orElse(defaultJvmHome)
    }

    @Override
    boolean canBeApplied(Project project) {
        return SystemPropertiesCompat.getBoolean(WORKAROUND_ENABLED_PROPERTY, project, true)
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

    @CacheableTransform
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
            ModuleDescriptor descriptor = captureModuleDescriptorWithoutVersion(moduleInfoFile)
            File descriptorData = new File(targetDir, "module-descriptor.txt")
            descriptorData.text = serializeDescriptor(descriptor)

            fileOperations.delete {
                delete(moduleInfoFile)
            }
        }

        private static ModuleDescriptor captureModuleDescriptorWithoutVersion(File moduleFile) {
            return ModuleDescriptor.read(ByteBuffer.wrap(Files.readAllBytes(moduleFile.toPath())))
        }

        @VisibleForTesting
        static String serializeDescriptor(ModuleDescriptor descriptor) {
            StringBuilder sb = new StringBuilder()

            if (descriptor.isOpen())
                sb.append("open ")
            sb.append("module { name: ").append(descriptor.name())
            if (!descriptor.requires().isEmpty())
                sb.append(", ").append(descriptor.requires().sort().collect { serializeRequires(it) })
            if (!descriptor.uses().isEmpty())
                sb.append(", uses: ").append(descriptor.uses().sort())
            if (!descriptor.exports().isEmpty())
                sb.append(", exports: ").append(descriptor.exports().sort().collect { serializeExports(it) })
            if (!descriptor.opens().isEmpty())
                sb.append(", opens: ").append(descriptor.opens().sort().collect { serializeOpens(it) })
            if (!descriptor.provides().isEmpty()) {
                sb.append(", provides: ").append(descriptor.provides().sort().collect { serializeProvides(it) })
            }
            sb.append(" }")
            return sb.toString()
        }

        private static String serializeRequires(ModuleDescriptor.Requires requires) {
            String requireString
            if (!requires.compiledVersion().empty) {
                requireString = requires.name() + " (@" + requires.compiledVersion() + ")"
            } else {
                requireString = requires.name()
            }
            return withSerializedMods(requires.modifiers(), requireString)
        }

        private static String serializeExports(ModuleDescriptor.Exports exports) {
            String s = withSerializedMods(exports.modifiers(), exports.source())
            if (exports.targets().isEmpty())
                return s;
            else
                return s + " to " + exports.targets().sort()
        }

        private static String serializeOpens(ModuleDescriptor.Opens opens) {
            String s = withSerializedMods(opens.modifiers(), opens.source())
            if (opens.targets().isEmpty())
                return s;
            else
                return s + " to " + opens.targets().sort()
        }

        private static String serializeProvides(ModuleDescriptor.Provides provides) {
            return provides.service() + " with " + Lists.newArrayList(provides.providers()).sort()
        }

        static <M> String withSerializedMods(Set<M> mods, String what) {
            return (Stream.concat(mods.stream().map(e -> e.toString()
                .toLowerCase(Locale.ROOT)).sorted(),
                Stream.of(what)))
                .collect(Collectors.joining(" "))
        }
    }
}
