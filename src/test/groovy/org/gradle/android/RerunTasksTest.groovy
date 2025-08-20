package org.gradle.android

import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume

@MultiVersionTest
class RerunTasksTest extends AbstractTest {

    def "test with configuration cache and --rerun-tasks works"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() >= VersionNumber.parse("4.2.0-alpha01"))

        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
                .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
                .withToolchainVersion("11")
                .withSourceCompatibility(org.gradle.api.JavaVersion.VERSION_11)
                .build()
                .writeProject()

        withGradleVersion(TestVersions.latestGradleVersion().version)
                .withProjectDir(projectDir)
                .withArguments("assembleDebug", "--stacktrace", "--rerun-tasks", "--configuration-cache")
                .build()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
                .withProjectDir(projectDir)
                .withArguments("assembleDebug", "--stacktrace", "--rerun-tasks", "--configuration-cache")
                .build()

        then:
        result.task(":library:assembleDebug").outcome == TaskOutcome.SUCCESS
        result.task(":library:compileDebugLibraryResources").outcome == TaskOutcome.SUCCESS
    }
}
