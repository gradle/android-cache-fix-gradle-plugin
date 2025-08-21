package org.gradle.android

import org.gradle.android.workarounds.JdkImageWorkaround
import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import spock.lang.Issue

@MultiVersionTest
class JdkImageWorkaroundTest extends AbstractTest {
    private static final String ZULU_PATH = "org.gradle.android.java_zulu_path"
    private static final String ZULU_ALT_PATH = "org.gradle.android.java_zulu_alt_path"

    def "jdkImage is normalized when using the same JDK version"() {
        def zuluPath = System.getProperty(ZULU_PATH)
        Assume.assumeTrue("Zulu path is not available", zuluPath != null && new File(zuluPath).exists())

        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withDatabindingDisabled() // Disabled due to https://issuetracker.google.com/issues/279710208
            .build()
            .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                    ["JDK": zuluPath]
            )
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                    ["JDK": zuluPath]
            )
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
    }

    def "jdkImage is normalized across same vendor similar JDK versions"() {
        def zuluPath = System.getProperty(ZULU_PATH)
        def zuluAltPath = System.getProperty(ZULU_ALT_PATH)
        Assume.assumeTrue("Zulu path is not available", zuluPath != null && new File(zuluPath).exists())
        Assume.assumeTrue("Zulu alternate path is not available", zuluAltPath != null && new File(zuluAltPath).exists())

        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withToolchainVersion("11")
            .withDatabindingDisabled() // Disabled due to https://issuetracker.google.com/issues/279710208
            .build()
            .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                ["JDK": zuluPath]
            )
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                ["JDK": zuluAltPath]
            )
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
    }

    def "workaround can be disabled via system property"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        Assume.assumeTrue(androidVersion >= VersionNumber.parse("7.1.0-alpha01"))
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withDatabindingDisabled() // Disabled due to https://issuetracker.google.com/issues/279710208
            .build()
            .writeProject()

        file("build.gradle") << """
            allprojects {
                tasks.withType(JavaCompile).configureEach { task ->
                    task.doFirst {
                        def originalProvider = task.options.compilerArgumentProviders.find { it.class.simpleName == "JdkImageInput" }
                        assert originalProvider != null
                        project.logger.warn "CommandLineArgumentProvider is \${originalProvider.class.simpleName}"

                        def shouldNotBePresent = task.options.compilerArgumentProviders.find { it.class.simpleName == "ExtractedJdkImageCommandLineProvider" }
                        assert shouldNotBePresent == null
                    }
                }
            }
        """

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache",
                "-D${JdkImageWorkaround.WORKAROUND_ENABLED_PROPERTY}=false"
            ).build()

        then:
        buildResult.output.contains("CommandLineArgumentProvider is JdkImageInput")
    }

    def "workaround is enabled when enabled via system property"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        Assume.assumeTrue(androidVersion >= VersionNumber.parse("7.1.0-alpha01"))
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withDatabindingDisabled() // Disabled due to https://issuetracker.google.com/issues/279710208
            .build()
            .writeProject()

        file("build.gradle") << """
            allprojects {
                tasks.withType(JavaCompile).configureEach { task ->
                    task.doFirst {
                        def workaroundProvider = task.options.compilerArgumentProviders.find { it.class.simpleName == "ExtractedJdkImageCommandLineProvider" }
                        assert workaroundProvider != null
                        project.logger.warn "CommandLineArgumentProvider is \${workaroundProvider.class.simpleName}"

                        def shouldNotBePresent = task.options.compilerArgumentProviders.find { it.class.simpleName == "JdkImageInput" }
                        assert shouldNotBePresent == null
                    }
                }
            }
        """

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache",
                "-D${JdkImageWorkaround.WORKAROUND_ENABLED_PROPERTY}=true"
            ).build()

        then:
        buildResult.output.contains("CommandLineArgumentProvider is ExtractedJdkImageCommandLineProvider")
    }

    @Issue("https://github.com/gradle/android-cache-fix-gradle-plugin/issues/307")
    def "jdkImage is normalized when toolchain is not specified"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
    }

    def "jdkImage is normalized when using different toolchain configuration"() {

        Assume.assumeTrue("Android Gradle Plugin < 8", androidVersion >= VersionNumber.parse("8.0"))

        def toolchainVersion = (androidVersion >= VersionNumber.parse("8.2.0")) ? "21" : "19"

        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withToolchainVersion(toolchainVersion)
            .withDatabindingDisabled()
            .build()
            .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "testDebug", "testRelease", "assemble",
                "--build-cache"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE

        buildResult.task(':app:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseUnitTestJavaWithJavac').outcome == TaskOutcome.FROM_CACHE

        where:
        androidVersion << TestVersions.latestAndroidVersions
    }
}
