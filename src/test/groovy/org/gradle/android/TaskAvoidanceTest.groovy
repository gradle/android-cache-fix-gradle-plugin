package org.gradle.android

import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

class TaskAvoidanceTest extends AbstractTest {
    @Unroll
    def "Source Tasks are avoided with #gradleVersion and Android plugin #androidVersion"() {
        assert gradleVersion instanceof GradleVersion
        assert androidVersion instanceof VersionNumber

        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withKotlinDisabled()
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
        [androidVersion, gradleVersion] << Versions.SUPPORTED_VERSIONS_MATRIX.entries().collect { [it.key, it.value] }
    }
}