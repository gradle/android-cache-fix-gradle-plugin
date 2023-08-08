package org.gradle.android

import org.gradle.testkit.runner.BuildResult

@MultiVersionTest
class ConfigurationCachingTest extends AbstractTest {
    private static final VersionNumber SUPPORTED_KOTLIN_VERSION = TestVersions.latestSupportedKotlinVersion()
    static final String CC_PROBLEMS_FOUND = "problems were found storing the configuration cache"

    def "plugin is compatible with configuration cache"() {
        
        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(TestVersions.latestAndroidVersionForCurrentJDK())
                .withKotlinVersion(SUPPORTED_KOTLIN_VERSION)
                .build()
                .writeProject()

        when:
        def result = withGradleVersion(TestVersions.latestGradleVersion().version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('--configuration-cache', 'assembleDebug')
                .build()

        then:
        assertNoConfigCacheProblemsFound(result)

        when:
        result = withGradleVersion(TestVersions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments('--configuration-cache', 'assembleDebug')
            .build()

        then:
        assertConfigurationCacheIsReused(result)
    }

    void assertConfigurationCacheIsReused(BuildResult result) {
        assert result.output.contains('Reusing configuration cache.')
    }

    void assertNoConfigCacheProblemsFound(BuildResult result) {
        assert !result.output.contains(CC_PROBLEMS_FOUND) || result.output.contains("0 ${CC_PROBLEMS_FOUND}")
    }
}
