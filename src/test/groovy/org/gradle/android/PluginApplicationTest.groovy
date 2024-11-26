package org.gradle.android

import spock.lang.Unroll

import static java.util.regex.Pattern.quote

class PluginApplicationTest extends AbstractTest {

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. For older Android Gradle Plugin versions, please use Android Cache Fix Plugin 2.4.6/

        where:
        androidVersion << ["4.2.2"]

    }
}
