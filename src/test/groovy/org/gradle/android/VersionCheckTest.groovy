package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class VersionCheckTest extends Specification {
    private static final int ANDROID_LICENSE_FILE_VERSION = 1

    @Rule TemporaryFolder temporaryFolder
    File projectDir
    File buildFile
    File defaultAndroidSdkHome = new File("${System.getProperty("user.home")}/Library/Android/sdk")

    def setup() {
        projectDir = temporaryFolder.newFolder()
        buildFile = file("build.gradle")
        createAndroidStructureAtRoot()
        configureAndroidSdk()
    }

    @Unroll
    def "works with #gradleVersion and Android plugin #androidVersion"() {
        buildFile << declarePlugin(androidVersion.toString())
        def gradleVerString = gradleVersion.version
        expect:
        def result = GradleRunner.create()
            .withGradleVersion(gradleVerString)
            .withProjectDir(projectDir)
            .withArguments("tasks", "-S")
            .withPluginClasspath()
            .forwardOutput()
            .withDebug(true)
            .build()
        !result.output.contains("not applying workarounds")

        where:
        // [gradleVersion, androidVersion] << GroovyCollections.combinations(SupportedVersions.GRADLE_VERSIONS, SupportedVersions.ANDROID_VERSIONS)
        gradleVersion = GradleVersion.current()
        androidVersion = VersionNumber.parse("3.0.0")
    }

    def "does not apply workarounds with Gradle 4.4"() {
        buildFile << declarePlugin("3.0.0")
        expect:
        def result = GradleRunner.create()
            .withGradleVersion("4.4-20171105235948+0000")
            .withProjectDir(projectDir)
            .withArguments("tasks")
            .withPluginClasspath()
            .build()
        result.output.contains("Gradle 4.4 is not supported by Android cache fix plugin, not applying workarounds")
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
            
            apply plugin: "com.android.application"
            
            android {
                compileSdkVersion 26
                buildToolsVersion "26.0.2"
            }
        """
    }

    def createAndroidStructureAtRoot() {
        dir('src/main/java/org/gradle/droid/test')
        def manifest = file('src/main/AndroidManifest.xml')
        manifest << androidManifest()
    }

    String androidManifest() {
        """<?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="org.gradle.droid.test">
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="@string/app_name"
                    android:theme="@style/AppTheme"
                >
                </application>
            </manifest>
        """
    }

    def configureAndroidSdk() {
        defaultAndroidSdkHome.mkdirs()
        File androidLicensesFolder = new File(defaultAndroidSdkHome, "licenses")
        androidLicensesFolder.mkdirs()
        File androidLicensesVersionFile = new File(androidLicensesFolder, "version")
        if (!androidLicensesVersionFile.exists() || Integer.valueOf(androidLicensesVersionFile.text) < ANDROID_LICENSE_FILE_VERSION) {

            androidLicensesVersionFile.text = ANDROID_LICENSE_FILE_VERSION
            new File(androidLicensesFolder, "android-sdk-license").text = "${System.lineSeparator()}d56f5187479451eabf01fb78af6dfcb131a6481e"
        }
        file('local.properties').text = "sdk.dir=${defaultAndroidSdkHome.absolutePath.replace(File.separatorChar, '/' as char)}"
    }

    def file(String path) {
        new File(projectDir, path)
    }

    def dir(String path) {
        def dir = file(path)
        dir.mkdirs()
        dir
    }
}
