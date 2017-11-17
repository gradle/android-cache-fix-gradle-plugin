package org.gradle.android

import spock.lang.Unroll

class PluginApplicationTest extends AbstractTest {

    def "does not apply workarounds with Gradle 4.4"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, "3.0.0").writeProject()
        expect:
        def result = withGradleVersion("4.4-20171105235948+0000")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .buildAndFail()
        result.output =~ /Gradle 4.4 is not supported by Android cache fix plugin. Supported Gradle versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./
    }

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, "2.3.0").writeProject()
        expect:
        def result = withGradleVersion("4.1")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .buildAndFail()
        result.output =~ /Android plugin 2.3.0 is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["2.3.0", "3.1.0-alpha01"]
    }
}
