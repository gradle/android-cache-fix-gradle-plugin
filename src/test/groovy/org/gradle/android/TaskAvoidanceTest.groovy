package org.gradle.android

import spock.lang.Unroll

@MultiVersionTest
class TaskAvoidanceTest extends AbstractTest {
    @Unroll
    def "Source Tasks are avoided with #gradleVersion and Android plugin #androidVersion"() {
        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withKotlinDisabled()
                .withToolchainVersion("11")
                .withSourceCompatibility(org.gradle.api.JavaVersion.VERSION_11)
                .build()
                .writeProject()

        file('build.gradle') << """
            allprojects {
              tasks.withType(SourceTask).configureEach {
                println "configuring \$it"
              }
            }
        """

        when:
        def result = withGradleVersion(gradleVersion.version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('help')
                .build()

        then:
        !result.output.contains("configuring")

        where:
        [androidVersion, gradleVersion] << TestVersions.allCandidateTestVersions.entries().collect { [it.key, it.value] }
    }
}
