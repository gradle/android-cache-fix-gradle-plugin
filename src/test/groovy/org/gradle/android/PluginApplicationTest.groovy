package org.gradle.android

import spock.lang.Unroll

import static java.util.regex.Pattern.quote

class PluginApplicationTest extends AbstractTest {

    @Unroll
    def "does not apply workarounds with Gradle #gradleVersion"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, "3.0.0").writeProject()
        expect:
        def result = withGradleVersion(gradleVersion)
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .buildAndFail()
        result.output =~ /Gradle ${quote(gradleVersion)} is not supported by Android cache fix plugin. Supported Gradle versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        gradleVersion << ["4.5-20171119235929+0000"]
    }

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, androidVersion).writeProject()
        expect:
        def result = withGradleVersion("4.1")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["2.3.0", "3.1.0-alpha01"]
    }
}
