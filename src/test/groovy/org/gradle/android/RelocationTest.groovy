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
        new SimpleAndroidApp(originalDir, cacheDir, androidVersion).writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(relocatedDir, cacheDir, androidVersion).writeProject()

        def expectedResults = expectedResults(androidVersion, gradleVersion)

        println expectedResults.describe()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        withGradleVersion(gradleVersion.version)
            .withProjectDir(originalDir)
            .withArguments("assemble", "--build-cache")
            .build()

        when:
        def result = withGradleVersion(gradleVersion.version)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "--build-cache")
            .build()

        then:
        expectedResults.verify(result)

        cleanup:
        originalDir.deleteDir()
        relocatedDir.deleteDir()

        where:
        [androidVersion, gradleVersion] << Versions.SUPPORTED_VERSIONS_MATRIX.entries().collect { [it.key, it.value] }
    }

    static class ExpectedResults {
        private final Map<String, TaskOutcome> outcomes

        ExpectedResults(Map<String, TaskOutcome> outcomes) {
            this.outcomes = outcomes
        }

        String describe() {
            "> Expecting ${outcomes.values().count(FROM_CACHE)} tasks out of ${outcomes.size()} to be cached"
        }

        boolean verify(BuildResult result) {
            boolean allMatched = true
            outcomes.each { taskName, expectedOutcome ->
                def taskOutcome = result.task(taskName)?.outcome
                if (taskOutcome != expectedOutcome) {
                    println "> Task '$taskName' was $taskOutcome but should have been $expectedOutcome"
                    allMatched = false
                }
            }
            return allMatched
        }
    }

    private static ExpectedResults expectedResults(VersionNumber androidVersion, GradleVersion gradleVersion) {
        def isAndroid30x = androidVersion <= android("3.0.1")
        def builder = ImmutableMap.<String, TaskOutcome>builder()
        builder.put(':app:assemble', SUCCESS)
        builder.put(':app:assembleDebug', SUCCESS)
        builder.put(':app:assembleRelease', SUCCESS)
        builder.put(':app:checkDebugManifest', isAndroid30x
            ? FROM_CACHE
            : SUCCESS)
        builder.put(':app:checkReleaseManifest', isAndroid30x
            ? FROM_CACHE
            : SUCCESS)
        builder.put(':app:compileDebugAidl', FROM_CACHE)
        builder.put(':app:compileDebugJavaWithJavac', FROM_CACHE)
        builder.put(':app:compileDebugNdk', NO_SOURCE)
        builder.put(':app:compileDebugRenderscript', FROM_CACHE)
        builder.put(':app:compileDebugShaders', FROM_CACHE)
        builder.put(':app:compileDebugSources', UP_TO_DATE)
        builder.put(':app:compileReleaseAidl', FROM_CACHE)
        builder.put(':app:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.put(':app:compileReleaseNdk', NO_SOURCE)
        builder.put(':app:compileReleaseRenderscript', FROM_CACHE)
        builder.put(':app:compileReleaseShaders', FROM_CACHE)
        builder.put(':app:compileReleaseSources', UP_TO_DATE)
        builder.put(':app:createDebugCompatibleScreenManifests', FROM_CACHE)
        builder.put(':app:createReleaseCompatibleScreenManifests', FROM_CACHE)
        builder.put(':app:generateDebugAssets', UP_TO_DATE)
        builder.put(':app:generateDebugBuildConfig', FROM_CACHE)
        builder.put(':app:generateDebugResources', UP_TO_DATE)
        builder.put(':app:generateDebugResValues', FROM_CACHE)
        builder.put(':app:generateDebugSources', SUCCESS)
        builder.put(':app:generateReleaseAssets', UP_TO_DATE)
        builder.put(':app:generateReleaseBuildConfig', FROM_CACHE)
        builder.put(':app:generateReleaseResources', UP_TO_DATE)
        builder.put(':app:generateReleaseResValues', FROM_CACHE)
        builder.put(':app:generateReleaseSources', SUCCESS)
        builder.put(':app:javaPreCompileDebug', FROM_CACHE)
        builder.put(':app:javaPreCompileRelease', FROM_CACHE)
        builder.put(':app:lintVitalRelease', SUCCESS)
        builder.put(':app:mergeDebugAssets', FROM_CACHE)
        builder.put(':app:mergeDebugJniLibFolders', FROM_CACHE)
        builder.put(':app:mergeDebugResources', FROM_CACHE)
        builder.put(':app:mergeDebugShaders', FROM_CACHE)
        builder.put(':app:mergeReleaseAssets', FROM_CACHE)
        builder.put(':app:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.put(':app:mergeReleaseResources', FROM_CACHE)
        builder.put(':app:mergeReleaseShaders', FROM_CACHE)
        builder.put(':app:packageDebug', SUCCESS)
        builder.put(':app:packageRelease', SUCCESS)
        builder.put(':app:preBuild', UP_TO_DATE)
        builder.put(':app:preDebugBuild', FROM_CACHE)
        builder.put(':app:prepareLintJar', SUCCESS)
        builder.put(':app:preReleaseBuild', FROM_CACHE)
        builder.put(':app:processDebugJavaRes', NO_SOURCE)
        builder.put(':app:processDebugManifest', FROM_CACHE)
        builder.put(':app:processDebugResources', FROM_CACHE)
        builder.put(':app:processReleaseJavaRes', NO_SOURCE)
        builder.put(':app:processReleaseManifest', FROM_CACHE)
        builder.put(':app:processReleaseResources', FROM_CACHE)
        builder.put(':app:splitsDiscoveryTaskDebug', FROM_CACHE)
        builder.put(':app:splitsDiscoveryTaskRelease', FROM_CACHE)
        builder.put(':app:transformClassesWithDexBuilderForDebug', SUCCESS)

        if (isAndroid30x) {
            builder.put(':app:transformClassesWithPreDexForRelease', SUCCESS)
            builder.put(':app:transformDexWithDexForRelease', SUCCESS)
        } else {
            builder.put(':app:transformClassesWithDexBuilderForRelease', SUCCESS)
            builder.put(':app:transformDexArchiveWithDexMergerForRelease', SUCCESS)
            builder.put(':app:transformDexArchiveWithExternalLibsDexMergerForRelease', SUCCESS)
        }

        builder.put(':app:transformDexArchiveWithDexMergerForDebug',
            androidVersion != android("3.0.0") || Boolean.getBoolean("travis") && gradleVersion <= gradle("4.1")
                ? SUCCESS
                : FROM_CACHE
        )

        builder.put(':app:transformNativeLibsWithMergeJniLibsForDebug', SUCCESS)
        builder.put(':app:transformNativeLibsWithMergeJniLibsForRelease', SUCCESS)
        builder.put(':app:transformResourcesWithMergeJavaResForDebug', SUCCESS)
        builder.put(':app:transformResourcesWithMergeJavaResForRelease', SUCCESS)
        builder.put(':app:validateSigningDebug', SUCCESS)
        builder.put(':library:assemble', SUCCESS)
        builder.put(':library:assembleDebug', SUCCESS)
        builder.put(':library:assembleRelease', SUCCESS)
        builder.put(':library:bundleDebug', SUCCESS)
        builder.put(':library:bundleRelease', SUCCESS)
        builder.put(':library:checkDebugManifest', isAndroid30x
            ? FROM_CACHE
            : SUCCESS)
        builder.put(':library:checkReleaseManifest', isAndroid30x
            ? FROM_CACHE
            : SUCCESS)
        builder.put(':library:compileDebugAidl', FROM_CACHE)
        builder.put(':library:compileDebugJavaWithJavac', FROM_CACHE)
        builder.put(':library:compileDebugNdk', NO_SOURCE)
        builder.put(':library:compileDebugRenderscript', FROM_CACHE)
        builder.put(':library:compileDebugShaders', FROM_CACHE)
        builder.put(':library:compileDebugSources', UP_TO_DATE)
        builder.put(':library:compileReleaseAidl', FROM_CACHE)
        builder.put(':library:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.put(':library:compileReleaseNdk', NO_SOURCE)
        builder.put(':library:compileReleaseRenderscript', FROM_CACHE)
        builder.put(':library:compileReleaseShaders', FROM_CACHE)
        builder.put(':library:compileReleaseSources', UP_TO_DATE)
        builder.put(':library:extractDebugAnnotations', FROM_CACHE)
        builder.put(':library:extractReleaseAnnotations', androidVersion >= VersionNumber.parse("3.1.0-alpha04")
            ? FROM_CACHE
            : SUCCESS
        )
        builder.put(':library:generateDebugAssets', UP_TO_DATE)
        builder.put(':library:generateDebugBuildConfig', FROM_CACHE)
        builder.put(':library:generateDebugResources', UP_TO_DATE)
        builder.put(':library:generateDebugResValues', FROM_CACHE)
        builder.put(':library:generateDebugSources', SUCCESS)
        builder.put(':library:generateReleaseAssets', UP_TO_DATE)
        builder.put(':library:generateReleaseBuildConfig', FROM_CACHE)
        builder.put(':library:generateReleaseResources', UP_TO_DATE)
        builder.put(':library:generateReleaseResValues', FROM_CACHE)
        builder.put(':library:generateReleaseSources', SUCCESS)
        builder.put(':library:javaPreCompileDebug', FROM_CACHE)
        builder.put(':library:javaPreCompileRelease', FROM_CACHE)

        if (isAndroid30x) {
            builder.put(':library:mergeDebugAssets', FROM_CACHE)
            builder.put(':library:mergeReleaseAssets', FROM_CACHE)
            // TODO This produces overlapping outputs in build/intermediates/typedefs.txt
            builder.put(':library:mergeReleaseResources', gradleVersion < gradle("4.2")
                ? FROM_CACHE
                : SUCCESS
            )
        } else {
            builder.put(':library:mergeReleaseResources', SUCCESS)
            builder.put(':library:packageDebugAssets', FROM_CACHE)
            builder.put(':library:packageReleaseAssets', FROM_CACHE)
        }

        builder.put(':library:mergeDebugConsumerProguardFiles', SUCCESS)
        builder.put(':library:mergeDebugJniLibFolders', FROM_CACHE)
        builder.put(':library:mergeDebugShaders', FROM_CACHE)
        builder.put(':library:mergeReleaseConsumerProguardFiles', SUCCESS)
        builder.put(':library:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.put(':library:mergeReleaseShaders', FROM_CACHE)
        builder.put(':library:packageDebugRenderscript', NO_SOURCE)
        builder.put(':library:packageDebugResources', FROM_CACHE)
        builder.put(':library:packageReleaseRenderscript', NO_SOURCE)
        builder.put(':library:packageReleaseResources', FROM_CACHE)
        builder.put(':library:platformAttrExtractor', FROM_CACHE)
        builder.put(':library:preBuild', UP_TO_DATE)
        builder.put(':library:preDebugBuild', UP_TO_DATE)
        builder.put(':library:prepareLintJar', SUCCESS)
        builder.put(':library:preReleaseBuild', UP_TO_DATE)
        builder.put(':library:processDebugJavaRes', NO_SOURCE)
        builder.put(':library:processDebugManifest', FROM_CACHE)

        if (isAndroid30x) {
            builder.put(':library:processDebugResources', FROM_CACHE)
            builder.put(':library:processReleaseResources', FROM_CACHE)
        } else {
            builder.put(':library:generateDebugRFile', FROM_CACHE)
            builder.put(':library:generateReleaseRFile', FROM_CACHE)
        }

        builder.put(':library:processReleaseJavaRes', NO_SOURCE)
        builder.put(':library:processReleaseManifest', FROM_CACHE)
        builder.put(':library:transformClassesAndResourcesWithPrepareIntermediateJarsForDebug', SUCCESS)
        builder.put(':library:transformClassesAndResourcesWithPrepareIntermediateJarsForRelease', SUCCESS)
        builder.put(':library:transformClassesAndResourcesWithSyncLibJarsForDebug', SUCCESS)
        builder.put(':library:transformClassesAndResourcesWithSyncLibJarsForRelease', SUCCESS)
        builder.put(':library:transformNativeLibsWithIntermediateJniLibsForDebug', SUCCESS)
        builder.put(':library:transformNativeLibsWithIntermediateJniLibsForRelease', SUCCESS)
        builder.put(':library:transformNativeLibsWithMergeJniLibsForDebug', SUCCESS)
        builder.put(':library:transformNativeLibsWithMergeJniLibsForRelease', SUCCESS)
        builder.put(':library:transformNativeLibsWithSyncJniLibsForDebug', SUCCESS)
        builder.put(':library:transformNativeLibsWithSyncJniLibsForRelease', SUCCESS)
        builder.put(':library:transformResourcesWithMergeJavaResForDebug', SUCCESS)
        builder.put(':library:transformResourcesWithMergeJavaResForRelease', SUCCESS)
        builder.put(':library:verifyReleaseResources', SUCCESS)
        new ExpectedResults(
            builder.build()
        )
    }
}
