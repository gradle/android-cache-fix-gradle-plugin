package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class RoomWorkaroundAbstractTest extends AbstractTest {
    static final String[] CLEAN_BUILD = ["clean", "testDebug", "testRelease", "assembleAndroidTest", "--build-cache", "--stacktrace"]
    static final String[] INCREMENTAL_DEBUG_BUILD = ["assembleDebug", "--build-cache", "--stacktrace"]
    static final List<String> ALL_PROJECTS = ["app", "library"]
    static final List<String> ALL_VARIANTS = ["debug", "release"]

    void assertAllVariantTasksHaveOutcome(BuildResult buildResult, TaskOutcome taskOutcome, List<String> projects, List<String> variants, Closure<String> taskPathTransform) {
        projects.each { project ->
            variants.each { variant ->
                assert buildResult.task(taskPathTransform.call(project, variant)).outcome == taskOutcome
            }
        }
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

}
