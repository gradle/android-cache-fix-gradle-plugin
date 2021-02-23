package org.gradle.android

import org.gradle.internal.impldep.org.junit.experimental.categories.Category
import spock.lang.Unroll

@Category(MultiVersionTest)
class TaskAvoidanceTest extends AbstractTest {
    @Unroll
    def "Source Tasks are avoided with #gradleVersion and Android plugin #androidVersion"() {
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
        [androidVersion, gradleVersion] << TestVersions.allSupportedVersionsForCurrentJDK.entries().collect { [it.key, it.value] }
    }
}
