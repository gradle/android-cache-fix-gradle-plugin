package org.gradle.android

import spock.lang.Unroll

import static java.util.regex.Pattern.quote

class PluginApplicationTest extends AbstractTest {

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Please check if a newer version of this plugin is available or override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["4.2.1"]

    }

    def "warns when version is not supported but within range"() {
        def notLatestPatchAndroidVersion = "7.0.3"
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion(notLatestPatchAndroidVersion)
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(notLatestPatchAndroidVersion).version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .build()
        result.output.readLines().findAll {
            it =~ /WARNING: Android plugin ${quote("7.0.3")} has not been tested with this version of the Android cache fix plugin, although it may work.  We test against only the latest patch release versions of Android Gradle plugin: ${Versions.SUPPORTED_ANDROID_VERSIONS.join(", ")}.  If 7.0.3 is newly released, we may not have had a chance to release a version tested against it yet.  Proceed with caution.  You can suppress this warning with with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./
        }.size() == 1
    }
}
