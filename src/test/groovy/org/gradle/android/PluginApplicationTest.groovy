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
    def "#desc warning for being useless with Android version #androidVersion (data binding: #dataBinding)"() {
        def projectDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(projectDir, cacheDir, androidVersion, dataBinding).writeProject()

        expect:
        def result = withGradleVersion(Iterables.getLast(Versions.SUPPORTED_GRADLE_VERSIONS).version)
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .withDebug(true)
            .build()
        if (message == null) {
            assert !result.output.contains("WARNING: Android cache-fix plugin is not required")
        } else {
            assert result.output.contains(message)
        }

        where:
        dataBinding | androidVersion       | message
        true        | "3.0.0"              | null
        false       | "3.0.0"              | null
        true        | "3.0.1"              | null
        false       | "3.0.1"              | null
        true        | "3.1.0"              | null
        false       | "3.1.0"              | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.1.0 or later, unless Android data binding is used."
        true        | "3.1.1"              | null
        false       | "3.1.1"              | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.1.1 or later, unless Android data binding is used."
        true        | "3.1.2"              | null
        false       | "3.1.2"              | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.1.2 or later, unless Android data binding is used."
        true        | "3.1.3"              | null
        false       | "3.1.3"              | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.1.3 or later, unless Android data binding is used."
        true        | "3.2.0-alpha18"      | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.2.0-alpha18 or later."
        false       | "3.2.0-alpha18"      | "WARNING: Android cache-fix plugin is not required for project ':library' when using Android plugin 3.2.0-alpha18 or later."
        desc = message == null ? "does not print" : "prints"
    }
}
