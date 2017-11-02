package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class BootClasspathRelocatabilityTest extends Specification {
    @Rule TemporaryFolder temporaryFolder

    def "android test"() {
        def projectDir = temporaryFolder.newFolder()
        def buildFile = new File(projectDir, "build.gradle")
        buildFile << """
            buildscript {
                repositories {
                    google()
                    jcenter()
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:3.0.0'
                }
            }
            
            plugins {
                id "org.gradle.android.cache-fix"
            }
            
            apply plugin: "java"
        """

        expect:
        GradleRunner.create()
            .withGradleVersion("4.3")
            .withProjectDir(projectDir)
            .withArguments("assemble")
            .withPluginClasspath()
            .build()
    }
}
