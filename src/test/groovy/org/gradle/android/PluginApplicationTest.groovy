package org.gradle.android

import spock.lang.Ignore
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
        androidVersion << ["3.4.1", "4.2.0-alpha01"]

    }

    // Temporarily ignored until we come up with a better way of testing this that doesn't introduce flakiness
    @Ignore
    def "warns when version is not supported but within range"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion("3.6.3")
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion(Versions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace", "-D${Versions.OMIT_VERSION_PROPERTY}=3.6.3")
            .build()
        result.output =~ /WARNING: Android plugin ${quote("3.6.3")} has not been tested with this version of the Android cache fix plugin, although it may work.  This is likely because it is newly released and we haven't had a chance to release a new version of Android cache fix that supports it.  Proceed with caution.  You can suppress this warning with with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./
    }
}
