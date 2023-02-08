package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@MultiVersionTest
class RoomSchemaLocationKspWorkaroundTest extends RoomWorkaroundAbstractTest {

    @Unroll
    def "schemas are generated with Ksp into task-specific directory and are cacheable with kotlin and kapt workers enabled (Android #androidVersion) (Kotlin #kotlinVersion) (Ksp #kspVersion)"() {
        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        def kspVersionNumber = VersionNumber.parse(kspVersion)
        // Using Kotlin 1.8.0 with Ksp 1.7.22 throws java.lang.NoClassDefFoundError: org/jetbrains/kotlin/gradle/dsl/KotlinJvmOptionsImpl
        Assume.assumeFalse(kotlinVersionNumber >= VersionNumber.parse("1.8.0") && kspVersionNumber == VersionNumber.parse("1.7.22-1.0.8"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(VersionNumber.parse(kotlinVersion))
            .withKspVersion(kspVersion)
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(CLEAN_BUILD)
            .build()

        then:
        assertCompileTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKspTasksHaveOutcome(buildResult, SUCCESS)
        assertKspAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKspUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKspSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        when:
        buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(CLEAN_BUILD)
            .build()

        then:
        assertCompileTasksHaveOutcome(buildResult, FROM_CACHE)
        assertCompileAndroidTestTasksHaveOutcome(buildResult, FROM_CACHE)
        assertCompileUnitTestTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKspTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKspAndroidTestTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKspUnitTestTasksHaveOutcome(buildResult, FROM_CACHE)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKspSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion, kspVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions, TestVersions.supportedKspVersions].combinations()
    }

    @Unroll
    def "schemas are correctly generated with Ksp when only one variant is built incrementally  (Android #androidVersion) (Kotlin #kotlinVersion) (Ksp #kspVersion)"() {

        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        def kspVersionNumber = VersionNumber.parse(kspVersion)

        // Using Kotlin 1.8.0 with Ksp 1.7.22 throws java.lang.NoClassDefFoundError: org/jetbrains/kotlin/gradle/dsl/KotlinJvmOptionsImpl
        Assume.assumeFalse(kotlinVersionNumber >= VersionNumber.parse("1.8.0") && kspVersionNumber == VersionNumber.parse("1.7.22-1.0.8"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(VersionNumber.parse(kotlinVersion))
            .withKspVersion(kspVersion)
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(CLEAN_BUILD)
            .build()

        then:
        assertCompileTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKspTasksHaveOutcome(buildResult, SUCCESS)
        assertKspAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKspUnitTestTasksHaveOutcome(buildResult, SUCCESS)

        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKspSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        and:
        assertKspSchemaContainsColumnFor('last_update', 'debug')

        and:
        assertMergedRoomSchemaContainsColumn("last_update")

        when:
        modifyRoomColumnName("last_update", "foo")
        buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(INCREMENTAL_DEBUG_BUILD)
            .build()

        then:
        assertCompileTasksHaveOutcome(buildResult, SUCCESS, ["debug"])
        assertKspTasksHaveOutcome(buildResult, SUCCESS, ["debug"])
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKspSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        and:
        assertKspSchemaContainsColumnFor('foo', 'debug')

        and:
        assertMergedRoomSchemaContainsColumn("foo")

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion, kspVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions, TestVersions.supportedKspVersions].combinations()

    }

    void assertNotExecuted(buildResult, String taskPath) {
        assert !buildResult.tasks.collect { it.path }.contains(taskPath)
    }

    void assertCompileTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:compile${variant.capitalize()}JavaWithJavac" }
    }

    void assertCompileAndroidTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, ["debug"]) { project, variant -> ":${project}:compile${variant.capitalize()}AndroidTestJavaWithJavac" }
    }

    void assertCompileUnitTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:compile${variant.capitalize()}UnitTestJavaWithJavac" }
    }

    void assertKspAndroidTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, ["debug"]) { project, variant -> ":${project}:ksp${variant.capitalize()}AndroidTestKotlin" }
    }

    void assertKspTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:ksp${variant.capitalize()}Kotlin" }
    }

    void assertKspUnitTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:ksp${variant.capitalize()}UnitTestKotlin" }
    }

    void assertKspSchemaOutputsExist() {
        assertKspSchemaOutputsExistFor("debug")
        assertKspSchemaOutputsExistFor("release")
    }

    void assertKspSchemaOutputsExistFor(String variant) {
        assertSchemasExist("app", "build/roomSchemas/ksp${variant.capitalize()}Kotlin")
        assertSchemasExist("library", "build/roomSchemas/ksp${variant.capitalize()}Kotlin")
    }

    void assertKspSchemaContainsColumnFor(String columnName, String variant) {
        assertRoomSchemaContainsColumn("app", "build/roomSchemas/ksp${variant.capitalize()}Kotlin", columnName)
        assertRoomSchemaContainsColumn("library", "build/roomSchemas/ksp${variant.capitalize()}Kotlin", columnName)
    }
}
