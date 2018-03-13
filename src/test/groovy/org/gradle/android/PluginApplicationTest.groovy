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
            .withArguments("tasks")
            .buildAndFail()
        result.output =~ /Android plugin ${quote(androidVersion)} is not supported by Android cache fix plugin. Supported Android plugin versions: .*. Override with -Dorg.gradle.android.cache-fix.ignoreVersionCheck=true./

        where:
        androidVersion << ["2.3.0", "3.1.0-alpha01"]
    }

    @Unroll
    def "does #description about being useless for Android version #androidVersion (data binding: #dataBinding)"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, androidVersion, dataBinding).writeProject()
        def message = "WARNING: Android cache-fix plugin is not required when using Android plugin $androidVersion or later, unless Android data binding is used."

        expect:
        def result = withGradleVersion(Iterables.getLast(Versions.SUPPORTED_GRADLE_VERSIONS).version)
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .withDebug(true)
            .build()
        if (warns) {
            assert result.output.contains(message)
        } else {
            assert !(result.output.contains(message))
        }

        where:
        warns | dataBinding | androidVersion
        false | true        | "3.0.0"
        false | false       | "3.0.0"
        false | true        | "3.0.1"
        false | false       | "3.0.1"
        false | true        | "3.1.0-beta3"
        true  | false       | "3.1.0-beta3"
        false | true        | "3.2.0-alpha06"
        true  | false       | "3.2.0-alpha06"
        description = warns ? "warn" : "not warn"
    }
}
