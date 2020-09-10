package org.gradle.android

import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

class TaskAvoidanceTest extends AbstractTest {
    @Unroll
    def "tasks are avoided with #gradleVersion and Android plugin #androidVersion"() {
        assert gradleVersion instanceof GradleVersion
        assert androidVersion instanceof VersionNumber

        println "> Using Android plugin $androidVersion"
        println "> Running with $gradleVersion"

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
        .withKotlinDisabled()
                .build()
                .writeProject()

        def originalSettings = file('build.gradle').text
        file('build.gradle').text = """
            ${originalSettings}

            allprojects {
              tasks.configureEach {
                println "configuring \$it"
              }
            }
        """

        when:
        def result = withGradleVersion(gradleVersion.version)
                .withProjectDir(temporaryFolder.root)
                .withArguments("help")
                .build()

        then:
        result.output.contains("configuring task ':help'")
        !result.output.contains("configuring task ':app:compileDebugJavaWithJavac'")
        !result.output.contains("configuring task ':app:compileDebugAndroidTestJavaWithJavac'")
        !result.output.contains("configuring task ':app:compileDebugUnitTestJavaWithJavac'")
        !result.output.contains("configuring task ':app:compileReleaseJavaWithJavac'")
        !result.output.contains("configuring task ':app:compileReleaseUnitTestJavaWithJavac'")
        !result.output.contains("configuring task ':library:compileDebugJavaWithJavac'")
        !result.output.contains("configuring task ':library:compileDebugAndroidTestJavaWithJavac'")
        !result.output.contains("configuring task ':library:compileDebugUnitTestJavaWithJavac'")
        !result.output.contains("configuring task ':library:compileReleaseJavaWithJavac'")
        !result.output.contains("configuring task ':library:compileReleaseUnitTestJavaWithJavac'")

        where:
        [androidVersion, gradleVersion] << Versions.SUPPORTED_VERSIONS_MATRIX.entries().collect { [it.key, it.value] }
    }
}
