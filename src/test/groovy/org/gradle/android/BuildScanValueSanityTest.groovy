package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Unroll

/**
 * There does not seem to be an easy way to verify that a custom value has actually
 * been added to a scan, so instead we just verify that our added build scan value
 * doesn't break with the latest versions of the build scan or gradle enterprise plugins
 * and check that it seems to have been applied.
 */
class BuildScanValueSanityTest extends AbstractTest {
    @Unroll
    def "build scan value is safe with Gradle 6 and gradle enterprise plugin version #pluginVersion"() {
        new SimpleAndroidApp(temporaryFolder.root, cacheDir, Versions.android("3.6.2"), true).writeProject()

        def originalSettings = file('settings.gradle').text
        file('settings.gradle').text = """
            plugins {
                id 'com.gradle.enterprise' version '${pluginVersion}'
            }

            ${originalSettings}
        """

        when:
        def result = GradleRunner.create().withGradleVersion("6.3")
            .withProjectDir(temporaryFolder.root)
            .withArguments("help", "--debug", "--stacktrace")
            .build()

        then:
        result.output.contains("Added build scan custom value for :app applied workarounds")
        result.output.contains("Added build scan custom value for :library applied workarounds")

        where:
        pluginVersion << ['3.2.1', '3.2', '3.1.1', '3.1', '3.0']
    }

    @Unroll
    def "build scan value is safe with Gradle 5 and build scan plugin version #pluginVersion"() {
        new SimpleAndroidApp(temporaryFolder.root, cacheDir, Versions.android("3.6.2"), true).writeProject()

        file('build.gradle') << """
            plugins {
                id 'com.gradle.build-scan' version '${pluginVersion}'
            }
        """

        when:
        def result = GradleRunner.create().withGradleVersion("5.6.4")
            .withProjectDir(temporaryFolder.root)
            .withArguments("help", "--debug", "--stacktrace")
            .build()

        then:
        result.output.contains("Added build scan custom value for :app applied workarounds")
        result.output.contains("Added build scan custom value for :library applied workarounds")

        where:
        pluginVersion << ['3.2.1', '3.2', '3.1.1', '3.1', '3.0', '2.4.2', '2.4.1', '2.4', '2.3', '2.2.1', '2.2', '2.1', '2.0.2']
    }

    File file(String path) {
        return new File(temporaryFolder.root, path)
    }
}
