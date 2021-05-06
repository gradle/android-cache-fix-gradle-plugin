package org.gradle.android

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.VersionNumber

@MultiVersionTest
class RerunTasksTest extends AbstractTest {

    def "test with --rerun-tasks works"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
                .withKotlinVersion(VersionNumber.parse("1.5.0"))
                .build()
                .writeProject()

        withGradleVersion("7.0")
                .withProjectDir(projectDir)
                .withArguments("assembleDebug", "--stacktrace", "--rerun-tasks", "--configuration-cache")
                .build()

        when:
        def result = withGradleVersion("7.0")
                .withProjectDir(projectDir)
                .withArguments("assembleDebug", "--stacktrace", "--rerun-tasks", "--configuration-cache")
                .build()

        then:
        result.task(":library:assembleDebug").outcome == TaskOutcome.SUCCESS
        result.task(":library:compileDebugLibraryResources").outcome == TaskOutcome.SUCCESS
    }
}
