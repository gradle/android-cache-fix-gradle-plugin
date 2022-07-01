package org.gradle.android

import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround
import org.junit.Assume
import spock.lang.Issue

@MultiVersionTest
class CompileLibraryResourcesWorkaroundTest extends AbstractTest {
    def "warns when experimental flags are not provided"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() >= Versions.android("7.0.0-alpha09"))
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() < Versions.android("7.2.0-beta01"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=false",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 1

        when:
        result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=false",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 1
    }

    def "does not warn when experimental flags are provided"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() >= Versions.android("7.0.0-alpha09"))
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() <= Versions.android("7.2.0-beta01"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=true",
                "-P${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=true",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 0
    }

    def "does not warn for versions that do not support experimental flag"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() < Versions.android("7.0.0-alpha09"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=false",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 0
    }

    @Issue("https://github.com/gradle/android-cache-fix-gradle-plugin/issues/234")
    def "does not warn about experimental flags when applied from a kotlin script plugin"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        file('buildSrc/src/main/kotlin').mkdirs()
        file('buildSrc/build.gradle.kts') << """
            plugins {
              `kotlin-dsl`
            }

            repositories {
                google()
                mavenCentral()
                maven {
                    url = uri("${SimpleAndroidApp.localRepo}")
                }
            }

            dependencies {
                implementation("${SimpleAndroidApp.pluginGroupId}:android-cache-fix-gradle-plugin:${SimpleAndroidApp.pluginVersion}")
                implementation("com.android.tools.build:gradle:${TestVersions.latestAndroidVersionForCurrentJDK().toString()}")
            }
        """
        file('buildSrc/src/main/kotlin/script-plugin.gradle.kts') << """
            plugins {
                id("org.gradle.android.cache-fix")
            }
        """
        file('app/build.gradle') << """
            apply plugin: "script-plugin"
        """

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround.ENABLE_SOURCE_SET_PATHS_MAP}=true",
                "-P${CompileLibraryResourcesWorkaround.CACHE_COMPILE_LIB_RESOURCES}=true",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 0
    }

    private static String warningForAndroidVersion(String androidVersion) {
        return Warnings.USE_COMPILE_LIBRARY_RESOURCES_EXPERIMENTAL.warning.replaceAll('Android plugin [^\\s]+', "Android plugin ${androidVersion}")
    }
}
