package org.gradle.android

import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import spock.lang.Issue
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@MultiVersionTest
class RoomSchemaLocationWorkaroundTest extends RoomWorkaroundAbstractTest {

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers enabled (Android #androidVersion) (Kotlin #kotlinVersion)"() {

        VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        checkMetadataIncompatibilityWithAgp8_1(androidVersion, kotlinVersionNumber)

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersionNumber)
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
        assertKaptTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

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
        assertKaptTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKaptUnitTestTasksHaveOutcome(buildResult, FROM_CACHE)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions.keySet()].combinations()
    }

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers disabled (Android #androidVersion) (Kotlin #kotlinVersion)"() {

        VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        checkMetadataIncompatibilityWithAgp8_1(androidVersion, kotlinVersionNumber)

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersionNumber)
            .withKaptWorkersDisabled()
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
        assertKaptTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

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
        assertKaptTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, FROM_CACHE)
        assertKaptUnitTestTasksHaveOutcome(buildResult, FROM_CACHE)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions.keySet()].combinations()
    }

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin disabled (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinDisabled()
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
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertCompileJavaSchemaOutputsExist()

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
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertCompileJavaSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        androidVersion << TestVersions.latestAndroidVersions
    }

    @Unroll
    def "workaround is not applied with older Kotlin plugin version (Kotlin #kotlinVersion)"() {
        Assume.assumeTrue(TestVersions.getLatestVersionForAndroid("7.3.1") != null)

        def androidVersion = TestVersions.getLatestVersionForAndroid("7.3.1")
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersion)
            .withRoomProcessingArgumentConfigured()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(CLEAN_BUILD + ['--info'] as String[])
            .build()

        then:
        assertCompileTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertCompileUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations') == null
        buildResult.task(':library:mergeRoomSchemaLocations') == null

        and:
        assertMergedSchemaOutputsExist()

        and:
        buildResult.output.contains("${RoomSchemaLocationWorkaround.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.6.0 or higher (found ${kotlinVersion}).")

        where:
        kotlinVersion << ["1.5.32"].collect { VersionNumber.parse(it) }
    }

    @Unroll
    def "schemas are correctly generated when only one variant is built incrementally (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(TestVersions.latestSupportedKotlinVersion())
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
        assertKaptTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        and:
        assertKaptSchemaContainsColumnFor('last_update', 'debug')

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
        assertKaptTasksHaveOutcome(buildResult, SUCCESS, ["debug"])
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        and:
        assertKaptSchemaContainsColumnFor('foo', 'debug')

        and:
        assertMergedRoomSchemaContainsColumn("foo")

        where:
        //noinspection GroovyAssignabilityCheck
        androidVersion << TestVersions.latestAndroidVersions
    }

    def "workaround throws an exception when room extension is not configured, but annotation processor argument is"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withRoomProcessingArgumentConfigured()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(CLEAN_BUILD)
            .buildAndFail()

        then:
        buildResult.output.contains("${RoomSchemaLocationWorkaround.class.simpleName} cannot be used with an explicit '${RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION}' annotation processor argument.  Please change this to configure the schema location directory via the 'room' project extension:")
    }

    def "builds with no errors when room extension is not configured and annotation processor argument is missing"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withNoRoomConfiguration()
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
        noExceptionThrown()

        and:
        assertNotExecuted(buildResult, ':app:mergeRoomSchemaLocations')
    }

    def "builds with no errors when room library is not on the classpath"() {
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withNoRoomLibrary()
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
        noExceptionThrown()

        and:
        assertNotExecuted(buildResult, ':app:mergeRoomSchemaLocations')

        and:
        !buildResult.output.contains('warning: The following options were not recognized by any processor: \'[room.schemaLocation')
    }

    @Unroll
    def "workaround can be disabled via system property (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withRoomProcessingArgumentConfigured()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache", "-D${RoomSchemaLocationWorkaround.WORKAROUND_ENABLED_PROPERTY}=false"
            ).build()

        then:
        noExceptionThrown()

        and:
        assertNotExecuted(buildResult, ':app:mergeRoomSchemaLocations')

        where:
        //noinspection GroovyAssignabilityCheck
        androidVersion << TestVersions.latestAndroidVersions
    }

    @Unroll
    def "workaround is enabled when enabled via system property (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments(
                "clean", "assemble",
                "--build-cache", "-D${RoomSchemaLocationWorkaround.WORKAROUND_ENABLED_PROPERTY}=true"
            ).build()

        then:
        noExceptionThrown()

        and:
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS

        where:
        //noinspection GroovyAssignabilityCheck
        androidVersion << TestVersions.latestAndroidVersions
    }

    @Issue("https://github.com/gradle/android-cache-fix-gradle-plugin/issues/353")
    @Unroll
    def "does not error when tasks are eagerly created (Android #androidVersion) (Kotlin #kotlinVersion)"() {

        VersionNumber kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        checkMetadataIncompatibilityWithAgp8_1(androidVersion, kotlinVersionNumber)

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(kotlinVersionNumber)
            .build()
            .writeProject()

        file('app/build.gradle') << eagerlyCreateJavaCompileTasks
        file('library/build.gradle') << eagerlyCreateJavaCompileTasks

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
        assertKaptTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptAndroidTestTasksHaveOutcome(buildResult, SUCCESS)
        assertKaptUnitTestTasksHaveOutcome(buildResult, SUCCESS)
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions.keySet()].combinations()
    }

    private static String getEagerlyCreateJavaCompileTasks() {
        """
            tasks.withType(JavaCompile) { println it.path }
        """
    }

    void assertKaptTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:kapt${variant.capitalize()}Kotlin" }
    }

    void assertKaptAndroidTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, ["debug"]) { project, variant -> ":${project}:kapt${variant.capitalize()}AndroidTestKotlin" }
    }

    void assertKaptUnitTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:kapt${variant.capitalize()}UnitTestKotlin" }
    }

    void assertKaptSchemaOutputsExist() {
        // Task specific schemas
        assertKaptSchemaOutputsExistFor("debug")
        assertKaptSchemaOutputsExistFor("release")
    }

    void assertKaptSchemaOutputsExistFor(String variant) {
        assertSchemasExist("app", "build/roomSchemas/kapt${variant.capitalize()}Kotlin")
        assertSchemasExist("library", "build/roomSchemas/kapt${variant.capitalize()}Kotlin")
    }

    void assertCompileJavaSchemaOutputsExist() {
        // Task specific schemas
        assertCompileJavaSchemaOutputExistsFor("debug")
        assertCompileJavaSchemaOutputExistsFor("release")
    }

    void assertCompileJavaSchemaOutputExistsFor(String variant) {
        assertSchemasExist("app", "build/roomSchemas/compile${variant.capitalize()}JavaWithJavac")
        assertSchemasExist("library", "build/roomSchemas/compile${variant.capitalize()}JavaWithJavac")
    }

    void assertKaptSchemaContainsColumnFor(String columnName, String variant) {
        assertRoomSchemaContainsColumn("app", "build/roomSchemas/kapt${variant.capitalize()}Kotlin", columnName)
        assertRoomSchemaContainsColumn("library", "build/roomSchemas/kapt${variant.capitalize()}Kotlin", columnName)
    }

    // Since 8.1.0-beta01, some AGP libraries are compiled with a newer Kotlin Compiler version.
    // Builds using KGP < 1.7.0 and AGP 8.1.0-beta01+ cause metadata incompatibilities.
    // https://issuetracker.google.com/issues/279710208
    private checkMetadataIncompatibilityWithAgp8_1(VersionNumber androidVersion, VersionNumber kotlinVersion) {
        Assume.assumeFalse(androidVersion >= VersionNumber.parse("8.1.0-beta01") && kotlinVersion < VersionNumber.parse("1.7.0"))
    }
}
