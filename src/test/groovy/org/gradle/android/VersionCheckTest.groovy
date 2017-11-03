package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import static groovy.util.GroovyCollections.combinations

class VersionCheckTest extends Specification {
    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildFile

    def setup() {
        projectDir = temporaryFolder.newFolder()
        buildFile = new File(projectDir, "build.gradle")
    }

    @Unroll
    def "works with #gradleVersion and Android plugin #androidVersion"() {
        buildFile << declarePlugin(androidVersion.toString())
        expect:
        def result = GradleRunner.create()
            .withGradleVersion(gradleVersion.version)
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .withPluginClasspath()
            .build()
        !result.output.contains("not applying workarounds")

        where:
        [gradleVersion, androidVersion] << combinations(SupportedVersions.GRADLE_VERSIONS, SupportedVersions.ANDROID_VERSIONS)
    }

    def "does not apply workarounds with Gradle 4.0"() {
        buildFile << declarePlugin("3.0.0")
        expect:
        def result = GradleRunner.create()
            .withGradleVersion("4.0")
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .withPluginClasspath()
            .build()
        result.output.contains("Gradle 4.0 is not supported by Android cache fix plugin, not applying workarounds")
    }

    @Ignore("Plugin mechanism overrides Android plugin version to 3.0.0")
    def "does not apply workarounds with Android 3.1.0-alpha01"() {
        buildFile << declarePlugin("3.1.0-alpha01")
        expect:
        def result = GradleRunner.create()
            .withGradleVersion("4.3")
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .withPluginClasspath()
            .withDebug(true)
            .build()
        result.output.contains("Android plugin 3.1.0-alpha01 is not supported by Android cache fix plugin, not applying workarounds")
    }

    def declarePlugin(String androidVersion) {
        """
            buildscript {
                repositories {
                    google()
                    jcenter()
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:$androidVersion'
                }
            }
            
            plugins {
                id "org.gradle.android.cache-fix"
            }
            
            apply plugin: "java"
        """
    }
}
