package org.gradle.android

import spock.lang.Unroll

import static java.util.regex.Pattern.quote

class PluginApplicationTest extends AbstractTest {

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(projectDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(VersionNumber.parse(TestVersions.kotlinVersion19))
            .build()
            .writeProject()

        expect:
        def result = withGradleVersion("8.14.3")
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. For older Android Gradle Plugin versions, please check #older-android-gradle-plugin-versions/

        where:
        androidVersion << ["8.3.0"]

    }
}
