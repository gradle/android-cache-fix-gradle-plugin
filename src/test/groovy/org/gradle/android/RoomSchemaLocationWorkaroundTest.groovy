package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class RoomSchemaLocationWorkaroundTest extends AbstractTest {
    @Unroll
    def "generates schemas into task-specific directory with kotlin and kapt workers enabled (Android #androidVersion)"() {
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
            .withArguments("assemble", "--stacktrace")
            .build()

        then:
        file("app/schemas/app/kaptDebugKotlin/org.gradle.android.example.app.AppDatabase/2.json").exists()
        file("app/schemas/app/kaptReleaseKotlin/org.gradle.android.example.app.AppDatabase/2.json").exists()
        file("library/schemas/library/kaptDebugKotlin/org.gradle.android.example.library.AppDatabase/2.json").exists()
        file("library/schemas/library/kaptReleaseKotlin/org.gradle.android.example.library.AppDatabase/2.json").exists()

        where:
        androidVersion << Versions.latestAndroidVersions
    }

    @Unroll
    def "ignores workaround with kotlin enabled and kapt workers disabled (Android #androidVersion)"() {
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
            .withArguments("assemble", "--stacktrace")
            .build()

        then:
        buildResult.output.contains("RoomSchemaLocationWorkaround only works when kapt.use.worker.api is set to true.  Ignoring.")

        where:
        androidVersion << Versions.latestAndroidVersions
    }

    @Unroll
    def "generates schemas into task-specific directory with kotlin disabled (Android #androidVersion)"() {
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
            .withArguments("assemble", "--stacktrace")
            .build()

        then:
        file("app/schemas/app/compileDebugJavaWithJavac/org.gradle.android.example.app.AppDatabase/2.json").exists()
        file("app/schemas/app/compileReleaseJavaWithJavac/org.gradle.android.example.app.AppDatabase/2.json").exists()
        file("library/schemas/library/compileDebugJavaWithJavac/org.gradle.android.example.library.AppDatabase/2.json").exists()
        file("library/schemas/library/compileReleaseJavaWithJavac/org.gradle.android.example.library.AppDatabase/2.json").exists()

        where:
        androidVersion << Versions.latestAndroidVersions
    }
}
