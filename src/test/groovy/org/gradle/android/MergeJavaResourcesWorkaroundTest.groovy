package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Issue
import spock.lang.Unroll


class MergeJavaResourcesWorkaroundTest extends AbstractTest {
    @Unroll
    @Issue('https://github.com/gradle/android-cache-fix-gradle-plugin/issues/78')
    def "workaround does not cause task to be skipped when inputs are empty (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("assemble", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:mergeDebugJavaResource').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:mergeReleaseJavaResource').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeDebugJavaResource').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeReleaseJavaResource').outcome == TaskOutcome.SUCCESS

        where:
        androidVersion << Versions.latestAndroidVersions
    }
}
