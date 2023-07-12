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
    def "schemas are generated with Ksp into task-specific directory and are cacheable (Android #androidVersion) (Kotlin #kotlinVersion)"() {
        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)

        // The Room workaround for KSP does not support Kotlin version 1.6.x or lower
        Assume.assumeTrue(kotlinVersionNumber >= VersionNumber.parse("1.7.0"))

        // AGP 8.2.0-alpha10 and higher require Kotlin 1.8.0 or higher
        if (androidVersion >= VersionNumber.parse("8.2.0-alpha10")) {
            Assume.assumeTrue(kotlinVersionNumber >= VersionNumber.parse("1.8.0"))
        }

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersionNumber)
            .withKspEnabled()
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

        and:
        assertKspSchemaContainsColumnFor('last_update', 'debug')

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions.keySet()].combinations()
    }

    @Unroll
    def "schemas are correctly generated with Ksp when only one variant is built incrementally (Android #androidVersion) (Kotlin #kotlinVersion)"() {
        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)

        // The Room workaround for KSP does not support Kotlin version 1.6.x or lower
        Assume.assumeTrue(kotlinVersionNumber >= VersionNumber.parse("1.7.0"))

        // AGP 8.2.0-alpha10 and higher require Kotlin 1.8.0 or higher
        if (androidVersion >= VersionNumber.parse("8.2.0-alpha10")) {
            Assume.assumeTrue(kotlinVersionNumber >= VersionNumber.parse("1.8.0"))
        }

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersionNumber)
            .withKspEnabled()
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
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions.keySet()].combinations()

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
