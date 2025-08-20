package org.gradle.android

import org.gradle.testkit.runner.TaskOutcome

// AGP 7.4 surfaced issues with the Plugin block configuration https://github.com/gradle/android-cache-fix-gradle-plugin/issues/412
// Current Gradle Runner tests are using buildscript block. This test covers the scenario of declaring the plugin in the
// plugins { block of the root build.gradle for these use cases:
// * Plugin apply by default
// * Plugin is not applied
@MultiVersionTest
class PluginBlockTest extends AbstractTest {

    def "project builds when plugin is defined in the Plugin block but is not applied"() {

        def projectDir = temporaryFolder.newFolder()

        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
            .withToolchainVersion("11")
            .withSourceCompatibility(org.gradle.api.JavaVersion.VERSION_11)
            .withPluginsBlockEnabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("assembleDebug", "--stacktrace", "--configuration-cache")
            .build()

        then:
        result.task(":library:assembleDebug").outcome == TaskOutcome.SUCCESS
        result.task(":library:compileDebugLibraryResources").outcome == TaskOutcome.SUCCESS
    }

    def "project builds when plugin is defined in the Plugin block and is applied"() {

        def projectDir = temporaryFolder.newFolder()

        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
            .withPluginsBlockEnabledApplyingThePlugin()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("assembleDebug", "--stacktrace", "--configuration-cache")
            .build()

        then:
        result.task(":library:assembleDebug").outcome == TaskOutcome.SUCCESS
        result.task(":library:compileDebugLibraryResources").outcome == TaskOutcome.SUCCESS
    }
}
