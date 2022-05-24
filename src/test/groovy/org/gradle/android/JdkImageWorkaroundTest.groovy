package org.gradle.android

import org.gradle.api.JavaVersion
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume

@MultiVersionTest
class JdkImageWorkaroundTest extends AbstractTest {
    private static final String ZULU_PATH = "org.gradle.android.java_zulu_path"
    private static final String ZULU_ALT_PATH = "org.gradle.android.java_zulu_alt_path"
    private static final String TEMURIN_PATH = "org.gradle.android.java_temurin_path"

    def "jdkImage is normalized when using the same JDK version"() {
        def zuluPath = System.getProperty(ZULU_PATH)
        Assume.assumeTrue("Zulu path is not available", zuluPath != null && new File(zuluPath).exists())

        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .withToolchainVersion("11")
            .withSourceCompatibility(JavaVersion.VERSION_1_9)
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
                "clean", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                    ["JDK": zuluPath]
            )
            .withArguments(
                "clean", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
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
            .withSourceCompatibility(JavaVersion.VERSION_1_9)
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
                "clean", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS

        when:
        buildResult = withGradleVersion(gradleVersion.version)
            .withProjectDir(temporaryFolder.root)
            .withEnvironment(
                System.getenv() +
                ["JDK": zuluAltPath]
            )
            .withArguments(
                "clean", "assemble",
                "--build-cache",
                "-Porg.gradle.java.installations.auto-detect=false",
                "-Porg.gradle.java.installations.fromEnv=JDK"
            ).build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
    }
}
