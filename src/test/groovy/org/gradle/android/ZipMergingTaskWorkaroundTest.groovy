package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

@MultiVersionTest
class ZipMergingTaskWorkaroundTest extends AbstractTest {
    @Unroll
    def "Apply workaround to avoid ZipMergingTask caching (Android #androidVersion)"() {
        def originalDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(originalDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .build()
            .writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(relocatedDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        String[] args = new String[]{"createFullJarDebug", "createFullJarRelease", "--build-cache", "--stacktrace"}

        withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(originalDir)
            .withArguments(args)
            .build()

        when:

        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(relocatedDir)
            .withArguments(args)
            .build()

        then:
        buildResult.task(':library:createFullJarDebug').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:createFullJarRelease').outcome == TaskOutcome.SUCCESS

        where:
        androidVersion << TestVersions.latestAndroidVersions
    }
}
