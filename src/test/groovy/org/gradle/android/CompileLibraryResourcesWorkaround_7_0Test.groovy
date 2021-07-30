package org.gradle.android

import org.gradle.android.workarounds.CompileLibraryResourcesWorkaround_7_0
import org.junit.Assume

@MultiVersionTest
class CompileLibraryResourcesWorkaround_7_0Test extends AbstractTest {
    def "warns when experimental flags are not provided"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() >= Versions.android("7.0.0-alpha09"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround_7_0.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround_7_0.CACHE_COMPILE_LIB_RESOURCES}=false",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 1

        when:
        result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround_7_0.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround_7_0.CACHE_COMPILE_LIB_RESOURCES}=false",
                'assembleDebug'
            )
            .build()

        then:
        result.output.count(warningForAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK().toString())) == 1
    }

    def "does not warn when experimental flags are provided"() {
        Assume.assumeTrue(TestVersions.latestAndroidVersionForCurrentJDK() >= Versions.android("7.0.0-alpha09"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
            .withKotlinDisabled()
            .build()
            .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "-P${CompileLibraryResourcesWorkaround_7_0.ENABLE_SOURCE_SET_PATHS_MAP}=true",
                "-P${CompileLibraryResourcesWorkaround_7_0.CACHE_COMPILE_LIB_RESOURCES}=true",
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
                "-P${CompileLibraryResourcesWorkaround_7_0.ENABLE_SOURCE_SET_PATHS_MAP}=false",
                "-P${CompileLibraryResourcesWorkaround_7_0.CACHE_COMPILE_LIB_RESOURCES}=false",
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
