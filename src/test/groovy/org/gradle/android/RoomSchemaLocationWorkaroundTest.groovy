package org.gradle.android

import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

@MultiVersionTest
class RoomSchemaLocationWorkaroundTest extends AbstractTest {
    private static final String[] CLEAN_BUILD = ["clean", "testDebug", "testRelease", "assembleAndroidTest", "--build-cache", "--stacktrace"]
    private static final String[] INCREMENTAL_DEBUG_BUILD = ["assembleDebug", "--build-cache", "--stacktrace"]
    private static final List<String> ALL_PROJECTS = ["app", "library"]
    private static final List<String> ALL_VARIANTS = ["debug", "release"]

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers enabled (Android #androidVersion) (Kotlin #kotlinVersion)"() {
        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        // There are kotlin module version errors when using older versions of kotlin with AGP 7.2.0+ in this configuration
        Assume.assumeFalse(androidVersion >= VersionNumber.parse("7.2.0-alpha01") && kotlinVersionNumber < VersionNumber.parse("1.5.0"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(VersionNumber.parse(kotlinVersion))
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
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions].combinations()
    }

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers disabled (Android #androidVersion) (Kotlin #kotlinVersion)"() {
        def kotlinVersionNumber = VersionNumber.parse(kotlinVersion)
        // There are kotlin module version errors when using older versions of kotlin with AGP 7.2.0+ in this configuration
        Assume.assumeFalse(androidVersion >= VersionNumber.parse("7.2.0-alpha01") && kotlinVersionNumber < VersionNumber.parse("1.5.0"))

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(VersionNumber.parse(kotlinVersion))
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
        [androidVersion, kotlinVersion] << [TestVersions.latestAndroidVersions, TestVersions.supportedKotlinVersions].combinations()
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
        Assume.assumeTrue(TestVersions.getLatestVersionForAndroid("3.6") != null)

        def androidVersion = TestVersions.getLatestVersionForAndroid("3.6")
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
        buildResult.output.contains("${RoomSchemaLocationWorkaround.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.3.70 or higher (found ${kotlinVersion}).")

        where:
        kotlinVersion << [ "1.3.61", "1.3.50" ].collect { VersionNumber.parse(it) }
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
        androidVersion << TestVersions.latestAndroidVersionswhere
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

    void assertNotExecuted(buildResult, String taskPath) {
        assert !buildResult.tasks.collect {it.path }.contains(taskPath)
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

    void assertKaptTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:kapt${variant.capitalize()}Kotlin" }
    }

    void assertKaptAndroidTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, ["debug"]) { project, variant -> ":${project}:kapt${variant.capitalize()}AndroidTestKotlin" }
    }

    void assertKaptUnitTestTasksHaveOutcome(BuildResult buildResult, TaskOutcome outcome, List<String> variants = ALL_VARIANTS) {
        assertAllVariantTasksHaveOutcome(buildResult, outcome, ALL_PROJECTS, variants) { project, variant -> ":${project}:kapt${variant.capitalize()}UnitTestKotlin" }
    }

    void assertAllVariantTasksHaveOutcome(BuildResult buildResult, TaskOutcome taskOutcome, List<String> projects, List<String> variants, Closure<String> taskPathTransform) {
        projects.each { project ->
            variants.each { variant ->
                assert buildResult.task(taskPathTransform.call(project, variant)).outcome == taskOutcome
            }
        }
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

    void assertMergedSchemaOutputsExist() {
        // Merged schemas
        assertSchemasExist("app", "schemas")
        assertSchemasExist("library", "schemas")
    }

    void assertSchemasExist(String project, String baseDirPath) {
        assert file("${roomSchemaDirPath(project, baseDirPath)}/1.json").exists()
        assert file("${roomSchemaDirPath(project, baseDirPath)}/2.json").exists()
        assertLegacySchemaUnchanged(file("${roomSchemaDirPath(project, baseDirPath)}/1.json"))
    }

    static String roomSchemaDirPath(String project, String baseDirPath) {
        return "${project}/${baseDirPath}/org.gradle.android.example.${project}.AppDatabase"
    }

    void modifyRoomColumnName(String oldColumnName, String newColumnName) {
        modifyRoomColumnName("app", oldColumnName, newColumnName)
        modifyRoomColumnName("library", oldColumnName, newColumnName)
    }

    void assertKaptSchemaContainsColumnFor(String columnName, String variant) {
        assertRoomSchemaContainsColumn("app", "build/roomSchemas/kapt${variant.capitalize()}Kotlin", columnName)
        assertRoomSchemaContainsColumn("library", "build/roomSchemas/kapt${variant.capitalize()}Kotlin", columnName)
    }

    void assertMergedRoomSchemaContainsColumn(String columnName) {
        assertRoomSchemaContainsColumn("app", 'schemas', columnName)
        assertRoomSchemaContainsColumn("library", 'schemas', columnName)
    }

    void assertRoomSchemaContainsColumn(String project, String baseDirPath, String columnName) {
        assert file("${roomSchemaDirPath(project, 'schemas')}/2.json").text.contains("\"columnName\": \"${columnName}\",")
    }

    void modifyRoomColumnName(String project, String oldColumnName, String newColumnName) {
        def migrationSourceFile = file("${project}/src/main/java/org/gradle/android/example/${project}/AppDatabase.java")
        migrationSourceFile.text = migrationSourceFile.text.replaceAll("ADD COLUMN ${oldColumnName}", "ADD COLUMN ${newColumnName}")
        assert migrationSourceFile.text.contains("ADD COLUMN ${newColumnName}")

        def schemaSourceFile = file("${project}/src/main/java/org/gradle/android/example/${project}/JavaUser.java")
        schemaSourceFile.text = schemaSourceFile.text.replaceAll("ColumnInfo\\(name = .${oldColumnName}.\\)", "ColumnInfo(name = \"${newColumnName}\")")
        assert schemaSourceFile.text.contains("@ColumnInfo(name = \"${newColumnName}\")")
    }

    static void assertLegacySchemaUnchanged(File legacySchemaFile) {
        assert legacySchemaFile.text == SimpleAndroidApp.legacySchemaContents
    }
}
