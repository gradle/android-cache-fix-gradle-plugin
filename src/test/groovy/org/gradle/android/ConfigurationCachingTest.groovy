package org.gradle.android

class ConfigurationCachingTest extends AbstractTest {
    def "Can run with configuration cache"() {
        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(Versions.latestAndroidVersion())
                .withKotlinDisabled()
                .build()
                .writeProject()

        when:
        def result = withGradleVersion(Versions.latestGradleVersion().version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('--configuration-cache', 'assembleDebug')
                .build()

        then:
        !result.output.contains("problems were found storing the configuration cache")
    }
}
