package org.gradle.android

import com.google.common.collect.ImmutableMap
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

import static org.gradle.android.Versions.android
import static org.gradle.android.Versions.gradle
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class RelocationTest extends AbstractTest {

    @Unroll
    def "simple Android app is relocatable with #gradleVersion and Android plugin #androidVersion"() {
        assert gradleVersion instanceof GradleVersion
        assert androidVersion instanceof VersionNumber

        println "> Using Android plugin $androidVersion"
        println "> Running with $gradleVersion"

        def originalDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(originalDir, cacheDir, androidVersion, true).writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(relocatedDir, cacheDir, androidVersion, true).writeProject()

        def expectedResults = expectedResults(androidVersion, gradleVersion)

        println expectedResults.describe()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        withGradleVersion(gradleVersion.version)
            .withProjectDir(originalDir)
            .withArguments("assemble", "--build-cache", "--stacktrace")
            .build()

        when:
        def result = withGradleVersion(gradleVersion.version)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "--build-cache", "--stacktrace")
            .build()

        then:
        expectedResults.verify(result)

        cleanup:
        originalDir.deleteDir()
        relocatedDir.deleteDir()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, gradleVersion] << Versions.SUPPORTED_VERSIONS_MATRIX.entries().collect { [it.key, it.value] }
    }

    static class ExpectedResults {
        private final Map<String, ExpectedOutcome> outcomes

        ExpectedResults(Map<String, ExpectedOutcome> outcomes) {
            this.outcomes = outcomes
        }

        String describe() {
            "> Expecting ${outcomes.values().count(FROM_CACHE)} tasks out of ${outcomes.size()} to be cached"
        }

        boolean verify(BuildResult result) {
            boolean allMatched = true
            def remainingTasks = result.tasks.collect { it.path }
            outcomes.each { taskName, expectedOutcome ->
                def taskOutcome = result.task(taskName)?.outcome
                if (expectedOutcome != ExpectedOutcome.UNKNOWN && taskOutcome.name() != expectedOutcome.name()) {
                    println "> Task '$taskName' was $taskOutcome but should have been $expectedOutcome"
                    allMatched = false
                }
                remainingTasks.remove(taskName)
            }
            if (!remainingTasks.empty) {
                remainingTasks.each { taskName ->
                    def taskOutcome = result.task(taskName)?.outcome
                    println "> Task '$taskName' executed with outcome $taskOutcome but was not expected"
                    allMatched = false
                }
            }
            return allMatched
        }
    }

    private enum ExpectedOutcome {
        SUCCESS,
        FAILED,
        UP_TO_DATE,
        SKIPPED,
        FROM_CACHE,
        NO_SOURCE,
        UNKNOWN; // represents tasks where the outcome is indeterminant
    }

    private static class ExpectedOutcomeBuilder extends ImmutableMap.Builder<String, ExpectedOutcome> {
        ImmutableMap.Builder<String, ExpectedOutcome> put(String key, TaskOutcome value) {
            return super.put(key, value ? value : ExpectedOutcome.UNKNOWN)
        }
    }

    private static ExpectedResults expectedResults(VersionNumber androidVersion, GradleVersion gradleVersion) {
        def isAndroid350to352 = androidVersion >= android("3.5.0") && androidVersion <= android("3.5.2")
        def isAndroid35x = androidVersion >= android("3.5.0") && androidVersion < android("3.6.0")
        def isAndroid36x = androidVersion >= android("3.6.0")
        def builder = new ExpectedOutcomeBuilder()

        if (isAndroid350to352) {
            builder.put(':library:createFullJarDebug', FROM_CACHE)
            builder.put(':library:createFullJarRelease', FROM_CACHE)
        }

        if (isAndroid35x) {
            builder.put(':app:checkDebugManifest', SUCCESS)
            builder.put(':app:checkReleaseManifest', SUCCESS)
            builder.put(':app:signingConfigWriterDebug', FROM_CACHE)
            builder.put(':app:signingConfigWriterRelease', FROM_CACHE)
            builder.put(':app:transformClassesWithDexBuilderForDebug', SUCCESS)
            builder.put(':app:transformClassesWithDexBuilderForRelease', SUCCESS)
            builder.put(':library:checkDebugManifest', SUCCESS)
            builder.put(':library:checkReleaseManifest', SUCCESS)
            builder.put(':library:parseDebugLibraryResources', FROM_CACHE)
            builder.put(':library:parseReleaseLibraryResources', FROM_CACHE)
            builder.put(':library:transformClassesAndResourcesWithSyncLibJarsForDebug', SUCCESS)
            builder.put(':library:transformClassesAndResourcesWithSyncLibJarsForRelease', SUCCESS)
            builder.put(':library:transformNativeLibsWithIntermediateJniLibsForDebug', SUCCESS)
            builder.put(':library:transformNativeLibsWithIntermediateJniLibsForRelease', SUCCESS)
            builder.put(':library:transformNativeLibsWithSyncJniLibsForDebug', SUCCESS)
            builder.put(':library:transformNativeLibsWithSyncJniLibsForRelease', SUCCESS)
        }

        if (isAndroid36x) {
            builder.put(':app:dexBuilderDebug', FROM_CACHE)
            builder.put(':app:dexBuilderRelease', FROM_CACHE)
            builder.put(':app:extractDeepLinksDebug', FROM_CACHE)
            builder.put(':app:extractDeepLinksRelease', FROM_CACHE)
            builder.put(':library:compileDebugLibraryResources', FROM_CACHE)
            builder.put(':library:compileReleaseLibraryResources', FROM_CACHE)
            builder.put(':library:copyDebugJniLibsProjectAndLocalJars', FROM_CACHE)
            builder.put(':library:copyDebugJniLibsProjectOnly', FROM_CACHE)
            builder.put(':library:copyReleaseJniLibsProjectAndLocalJars', FROM_CACHE)
            builder.put(':library:copyReleaseJniLibsProjectOnly', FROM_CACHE)
            builder.put(':library:extractDeepLinksDebug', FROM_CACHE)
            builder.put(':library:extractDeepLinksRelease', FROM_CACHE)
            builder.put(':library:parseDebugLocalResources', FROM_CACHE)
            builder.put(':library:parseReleaseLocalResources', FROM_CACHE)
            builder.put(':library:syncDebugLibJars', SUCCESS)
            builder.put(':library:syncReleaseLibJars', SUCCESS)
        }

        builder.put(':app:assemble', SUCCESS)
        builder.put(':app:assembleDebug', SUCCESS)
        builder.put(':app:assembleRelease', SUCCESS)
        builder.put(':app:checkDebugDuplicateClasses', FROM_CACHE)
        builder.put(':app:checkReleaseDuplicateClasses', FROM_CACHE)
        builder.put(':app:compileDebugAidl', NO_SOURCE)
        builder.put(':app:compileDebugJavaWithJavac', FROM_CACHE)
        builder.put(':app:compileDebugKotlin', FROM_CACHE)
        builder.put(':app:compileDebugRenderscript', NO_SOURCE)
        builder.put(':app:compileDebugShaders', FROM_CACHE)
        builder.put(':app:compileDebugSources', UP_TO_DATE)
        builder.put(':app:compileReleaseAidl', NO_SOURCE)
        builder.put(':app:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.put(':app:compileReleaseKotlin', FROM_CACHE)
        builder.put(':app:compileReleaseRenderscript', NO_SOURCE)
        builder.put(':app:compileReleaseShaders', FROM_CACHE)
        builder.put(':app:compileReleaseSources', UP_TO_DATE)
        builder.put(':app:createDebugCompatibleScreenManifests', FROM_CACHE)
        builder.put(':app:createReleaseCompatibleScreenManifests', FROM_CACHE)
        builder.put(':app:dataBindingExportBuildInfoDebug', SUCCESS)
        builder.put(':app:dataBindingExportBuildInfoRelease', SUCCESS)
        builder.put(':app:dataBindingExportFeaturePackageIdsDebug', FROM_CACHE)
        builder.put(':app:dataBindingExportFeaturePackageIdsRelease', FROM_CACHE)
        builder.put(':app:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.put(':app:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.put(':app:dataBindingMergeDependencyArtifactsDebug', FROM_CACHE)
        builder.put(':app:dataBindingMergeDependencyArtifactsRelease', FROM_CACHE)
        builder.put(':app:dataBindingMergeGenClassesDebug', SUCCESS)
        builder.put(':app:dataBindingMergeGenClassesRelease', SUCCESS)
        builder.put(':app:generateDebugAssets', UP_TO_DATE)
        builder.put(':app:generateDebugBuildConfig', FROM_CACHE)
        builder.put(':app:generateDebugResValues', FROM_CACHE)
        builder.put(':app:generateDebugResources', UP_TO_DATE)
        builder.put(':app:generateDebugSources', SUCCESS)
        builder.put(':app:generateReleaseAssets', UP_TO_DATE)
        builder.put(':app:generateReleaseBuildConfig', FROM_CACHE)
        builder.put(':app:generateReleaseResValues', FROM_CACHE)
        builder.put(':app:generateReleaseResources', UP_TO_DATE)
        builder.put(':app:generateReleaseSources', SUCCESS)
        builder.put(':app:javaPreCompileDebug', FROM_CACHE)
        builder.put(':app:javaPreCompileRelease', FROM_CACHE)
        builder.put(':app:lintVitalRelease', SUCCESS)
        builder.put(':app:mainApkListPersistenceDebug', FROM_CACHE)
        builder.put(':app:mainApkListPersistenceRelease', FROM_CACHE)
        builder.put(':app:mergeDebugAssets', FROM_CACHE)
        builder.put(':app:mergeDebugJavaResource', SUCCESS)
        builder.put(':app:mergeDebugJniLibFolders', FROM_CACHE)
        builder.put(':app:mergeDebugNativeLibs', FROM_CACHE)
        builder.put(':app:mergeDebugResources', FROM_CACHE)
        builder.put(':app:mergeDebugShaders', FROM_CACHE)
        builder.put(':app:mergeDexRelease', FROM_CACHE)
        builder.put(':app:mergeExtDexDebug', FROM_CACHE)
        builder.put(':app:mergeExtDexRelease', FROM_CACHE)
        builder.put(':app:mergeLibDexDebug', FROM_CACHE)
        builder.put(':app:mergeProjectDexDebug', FROM_CACHE)
        builder.put(':app:mergeReleaseAssets', FROM_CACHE)
        builder.put(':app:mergeReleaseJavaResource', SUCCESS)
        builder.put(':app:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.put(':app:mergeReleaseNativeLibs', FROM_CACHE)
        builder.put(':app:mergeReleaseResources', FROM_CACHE)
        builder.put(':app:mergeReleaseShaders', FROM_CACHE)
        builder.put(':app:packageDebug', SUCCESS)
        builder.put(':app:packageRelease', SUCCESS)
        builder.put(':app:preBuild', UP_TO_DATE)
        builder.put(':app:preDebugBuild', UP_TO_DATE)
        builder.put(':app:preReleaseBuild', UP_TO_DATE)
        builder.put(':app:prepareLintJar', SUCCESS)
        builder.put(':app:prepareLintJarForPublish', SUCCESS)
        builder.put(':app:processDebugJavaRes', NO_SOURCE)
        builder.put(':app:processDebugManifest', FROM_CACHE)
        builder.put(':app:processDebugResources', FROM_CACHE)
        builder.put(':app:processReleaseJavaRes', NO_SOURCE)
        builder.put(':app:processReleaseManifest', FROM_CACHE)
        builder.put(':app:processReleaseResources', FROM_CACHE)
        builder.put(':app:stripDebugDebugSymbols', FROM_CACHE)
        builder.put(':app:stripReleaseDebugSymbols', FROM_CACHE)
        builder.put(':app:validateSigningDebug', FROM_CACHE)
        builder.put(':library:assemble', SUCCESS)
        builder.put(':library:assembleDebug', SUCCESS)
        builder.put(':library:assembleRelease', SUCCESS)
        builder.put(':library:bundleDebugAar', SUCCESS)
        builder.put(':library:bundleLibCompileDebug', SUCCESS)
        builder.put(':library:bundleLibCompileRelease', SUCCESS)
        builder.put(':library:bundleLibResDebug', SUCCESS)
        builder.put(':library:bundleLibResRelease', SUCCESS)
        builder.put(':library:bundleLibRuntimeDebug', SUCCESS)
        builder.put(':library:bundleLibRuntimeRelease', SUCCESS)
        builder.put(':library:bundleReleaseAar', SUCCESS)
        builder.put(':library:compileDebugAidl', NO_SOURCE)
        builder.put(':library:compileDebugJavaWithJavac', FROM_CACHE)
        builder.put(':library:compileDebugKotlin', FROM_CACHE)
        builder.put(':library:compileDebugRenderscript', NO_SOURCE)
        builder.put(':library:compileDebugShaders', FROM_CACHE)
        builder.put(':library:compileDebugSources', UP_TO_DATE)
        builder.put(':library:compileReleaseAidl', NO_SOURCE)
        builder.put(':library:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.put(':library:compileReleaseKotlin', FROM_CACHE)
        builder.put(':library:compileReleaseRenderscript', NO_SOURCE)
        builder.put(':library:compileReleaseShaders', FROM_CACHE)
        builder.put(':library:compileReleaseSources', UP_TO_DATE)
        builder.put(':library:dataBindingExportBuildInfoDebug', SUCCESS)
        builder.put(':library:dataBindingExportBuildInfoRelease', SUCCESS)
        builder.put(':library:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.put(':library:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.put(':library:dataBindingMergeDependencyArtifactsDebug', FROM_CACHE)
        builder.put(':library:dataBindingMergeDependencyArtifactsRelease', FROM_CACHE)
        builder.put(':library:dataBindingMergeGenClassesDebug', SUCCESS)
        builder.put(':library:dataBindingMergeGenClassesRelease', SUCCESS)
        builder.put(':library:extractDebugAnnotations', FROM_CACHE)
        builder.put(':library:extractReleaseAnnotations', FROM_CACHE)
        builder.put(':library:generateDebugAssets', UP_TO_DATE)
        builder.put(':library:generateDebugBuildConfig', FROM_CACHE)
        builder.put(':library:generateDebugRFile', FROM_CACHE)
        builder.put(':library:generateDebugResValues', FROM_CACHE)
        builder.put(':library:generateDebugResources', UP_TO_DATE)
        builder.put(':library:generateDebugSources', SUCCESS)
        builder.put(':library:generateReleaseAssets', UP_TO_DATE)
        builder.put(':library:generateReleaseBuildConfig', FROM_CACHE)
        builder.put(':library:generateReleaseRFile', FROM_CACHE)
        builder.put(':library:generateReleaseResValues', FROM_CACHE)
        builder.put(':library:generateReleaseResources', UP_TO_DATE)
        builder.put(':library:generateReleaseSources', SUCCESS)
        builder.put(':library:javaPreCompileDebug', FROM_CACHE)
        builder.put(':library:javaPreCompileRelease', FROM_CACHE)
        builder.put(':library:mergeDebugConsumerProguardFiles', SUCCESS)
        builder.put(':library:mergeDebugGeneratedProguardFiles', SUCCESS)
        builder.put(':library:mergeDebugJavaResource', SUCCESS)
        builder.put(':library:mergeDebugJniLibFolders', FROM_CACHE)
        builder.put(':library:mergeDebugNativeLibs', FROM_CACHE)
        builder.put(':library:mergeDebugShaders', FROM_CACHE)
        builder.put(':library:mergeReleaseConsumerProguardFiles', SUCCESS)
        builder.put(':library:mergeReleaseGeneratedProguardFiles', SUCCESS)
        builder.put(':library:mergeReleaseJavaResource', SUCCESS)
        builder.put(':library:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.put(':library:mergeReleaseNativeLibs', FROM_CACHE)
        builder.put(':library:mergeReleaseResources', FROM_CACHE)
        builder.put(':library:mergeReleaseShaders', FROM_CACHE)
        builder.put(':library:packageDebugAssets', FROM_CACHE)
        builder.put(':library:packageDebugRenderscript', NO_SOURCE)
        builder.put(':library:packageDebugResources', FROM_CACHE)
        builder.put(':library:packageReleaseAssets', FROM_CACHE)
        builder.put(':library:packageReleaseRenderscript', NO_SOURCE)
        builder.put(':library:packageReleaseResources', FROM_CACHE)
        builder.put(':library:preBuild', UP_TO_DATE)
        builder.put(':library:preDebugBuild', UP_TO_DATE)
        builder.put(':library:preReleaseBuild', UP_TO_DATE)
        builder.put(':library:prepareLintJar', SUCCESS)
        builder.put(':library:prepareLintJarForPublish', SUCCESS)
        builder.put(':library:processDebugJavaRes', NO_SOURCE)
        builder.put(':library:processDebugManifest', FROM_CACHE)
        builder.put(':library:processReleaseJavaRes', NO_SOURCE)
        builder.put(':library:processReleaseManifest', FROM_CACHE)
        builder.put(':library:stripDebugDebugSymbols', FROM_CACHE)
        builder.put(':library:stripReleaseDebugSymbols', FROM_CACHE)
        // the outcome of this task is not consistent
        builder.put(':library:verifyReleaseResources', null)
        new ExpectedResults(
            builder.build()
        )
    }
}
