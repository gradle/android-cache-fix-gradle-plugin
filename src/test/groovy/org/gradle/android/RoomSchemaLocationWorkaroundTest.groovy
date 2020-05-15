package org.gradle.android

import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.VersionNumber
import spock.lang.Unroll

class RoomSchemaLocationWorkaroundTest extends AbstractTest {
    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers enabled (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        when:
        buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("clean", "assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:kaptDebugKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:kaptReleaseKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:kaptDebugKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:kaptReleaseKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        androidVersion << Versions.latestAndroidVersions
    }

    @Unroll
    def "schemas are generated into task-specific directory and are cacheable with kotlin and kapt workers disabled (Android #androidVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKaptWorkersDisabled()
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        when:
        buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("clean", "assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:kaptDebugKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:kaptReleaseKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:kaptDebugKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:kaptReleaseKotlin').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertKaptSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        androidVersion << Versions.latestAndroidVersions
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
        BuildResult buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertCompileJavaSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        when:
        buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("clean", "assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        buildResult.task(':app:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileDebugJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':library:compileReleaseJavaWithJavac').outcome == TaskOutcome.FROM_CACHE
        buildResult.task(':app:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:mergeRoomSchemaLocations').outcome == TaskOutcome.SUCCESS

        and:
        assertCompileJavaSchemaOutputsExist()

        and:
        assertMergedSchemaOutputsExist()

        where:
        androidVersion << Versions.latestAndroidVersions
    }

    @Unroll
    def "workaround is not applied with older Kotlin plugin version (Kotlin #kotlinVersion)"() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
            .withAndroidVersion(Versions.getLatestVersionForAndroid("3.6"))
            .withKotlinVersion(kotlinVersion)
            .build()
            .writeProject()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        when:
        BuildResult buildResult = withGradleVersion(Versions.latestGradleVersion().version)
            .forwardOutput()
            .withProjectDir(temporaryFolder.root)
            .withArguments("assemble", "--build-cache", "--stacktrace", "--info")
            .build()

        then:
        buildResult.task(':app:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptDebugKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:kaptReleaseKotlin').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:mergeRoomSchemaLocations') == null
        buildResult.task(':library:mergeRoomSchemaLocations') == null

        and:
        assertMergedSchemaOutputsExist()

        and:
        buildResult.output.contains("${RoomSchemaLocationWorkaround.class.simpleName} is only compatible with Kotlin Gradle plugin version 1.3.70 or higher (found ${kotlinVersion}).")

        where:
        kotlinVersion << [ "1.3.61", "1.3.50" ].collect { VersionNumber.parse(it) }
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
        assert file("${project}/${baseDirPath}/org.gradle.android.example.${project}.AppDatabase/1.json").exists()
        assert file("${project}/${baseDirPath}/org.gradle.android.example.${project}.AppDatabase/2.json").exists()
    }
}
