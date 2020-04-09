package org.gradle.android

import org.gradle.internal.impldep.com.google.common.collect.Iterables
import spock.lang.Unroll

import static java.util.regex.Pattern.quote

class PluginApplicationTest extends AbstractTest {

    @Unroll
    def "does not apply workarounds with Android #androidVersion"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, androidVersion, true).writeProject()
        expect:
        def result = withGradleVersion(Iterables.getLast(Versions.SUPPORTED_GRADLE_VERSIONS).version)
            .withProjectDir(projectDir)
            .withArguments("tasks", "--stacktrace")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["3.4.1", "4.1.0-alpha01"]
    }
}
