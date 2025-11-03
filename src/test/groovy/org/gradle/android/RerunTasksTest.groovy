package org.gradle.android

import org.gradle.testkit.runner.TaskOutcome

@MultiVersionTest
class RerunTasksTest extends AbstractTest {

    def "test with configuration cache and --rerun-tasks works"() {

        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
                .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
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
