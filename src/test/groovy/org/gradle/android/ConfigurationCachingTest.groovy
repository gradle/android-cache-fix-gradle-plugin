package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.util.VersionNumber

class ConfigurationCachingTest extends AbstractTest {
    private static final VersionNumber SUPPORTED_KOTLIN_VERSION = VersionNumber.parse("1.4.30")

    def "plugin is compatible with configuration cache"() {
        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(Versions.latestAndroidVersion())
                .withKotlinVersion(SUPPORTED_KOTLIN_VERSION)
                .build()
                .writeProject()

        when:
        def result = withGradleVersion(Versions.latestGradleVersion().version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('--configuration-cache', 'assembleDebug')
                .build()

        then:
        !result.output.contains("problems were found storing the configuration cache")

        when:
        result = withGradleVersion(Versions.latestGradleVersion().version)
            .withProjectDir(temporaryFolder.root)
            .withArguments('--configuration-cache', 'assembleDebug')
            .build()

        then:
        assertConfigurationCacheIsReused(result)
    }

    void assertConfigurationCacheIsReused(BuildResult result) {
        assert result.output.contains('Reusing configuration cache.')
    }
}
