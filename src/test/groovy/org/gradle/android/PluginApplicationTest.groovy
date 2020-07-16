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
        def result = withGradleVersion(Versions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["3.4.1"]

    }

    def "warns when version is not supported but within range"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion("3.6.1")
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion(Versions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .build()
        result.output =~ /WARNING: Android plugin ${quote("3.6.1")} has not been tested with this version of the Android cache fix plugin, although it may work.  We test against only the latest patch release versions of Android Gradle plugin: ${Versions.SUPPORTED_ANDROID_VERSIONS.join(", ")}.  If 3.6.1 is newly released, we may not have had a chance to release a version tested against it yet.  Proceed with caution.  You can suppress this warning with with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./
    }
}
