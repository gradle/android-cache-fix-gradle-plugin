package org.gradle.android

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class RelocationTest extends AbstractTest {

    @Unroll
    def "simple Android app is relocatable with #gradleVersion and Android plugin #androidVersion"() {
        assert gradleVersion instanceof GradleVersion
        assert androidVersion instanceof VersionNumber

        def originalDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(originalDir, cacheDir, androidVersion).writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(relocatedDir, cacheDir, androidVersion).writeProject()

        def expectedResults = EXPECTED_RESULTS[androidVersion]

        println expectedResults.describe(gradleVersion)

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
        expectedResults.verify(result, gradleVersion)

        where:
        [androidVersion, gradleVersion] << Versions.SUPPORTED_VERSIONS_MATRIX.entries().collect { [it.key, it.value] }
    }

    interface DynamicTaskOutcome {
        TaskOutcome evaluate(GradleVersion gradleVersion)
    }

    static class ExpectedResults {
        private final Map<String, ? extends Object> outcomes

        ExpectedResults(Map<String, ? extends Object> outcomes) {
            this.outcomes = outcomes
        }

        String describe(GradleVersion gradleVersion) {
            "Expecting ${outcomes.values().findAll { resolve(it, gradleVersion) == FROM_CACHE }.size()} tasks out of ${outcomes.size()} to be cached"
        }

        boolean verify(BuildResult result, GradleVersion gradleVersion) {
            boolean allMatched = true
            outcomes.each { taskName, outcome ->
                def expectedOutcome = resolve(outcome, gradleVersion)
                def taskOutcome = result.task(taskName).outcome
                if (taskOutcome != expectedOutcome) {
                    println "> Task '$taskName' was $taskOutcome but should have been $expectedOutcome"
                    allMatched = false
                }
            }
            return allMatched
        }

        TaskOutcome resolve(Object outcome, GradleVersion gradleVersion) {
            if (outcome instanceof DynamicTaskOutcome) {
                return outcome.evaluate(gradleVersion)
            }
            if (outcome instanceof TaskOutcome) {
                return outcome
            }
            throw new AssertionError("$outcome - ${outcome.class.name}")
        }
    }

    static final Map<VersionNumber, ExpectedResults> EXPECTED_RESULTS = [
        (VersionNumber.parse("3.0.0")): new ExpectedResults([
            ':app:assemble': SUCCESS,
            ':app:assembleDebug': SUCCESS,
            ':app:assembleRelease': SUCCESS,
            ':app:checkDebugManifest': FROM_CACHE,
            ':app:checkReleaseManifest': FROM_CACHE,
            ':app:compileDebugAidl': FROM_CACHE,
            ':app:compileDebugJavaWithJavac': FROM_CACHE,
            ':app:compileDebugNdk': NO_SOURCE,
            ':app:compileDebugRenderscript': FROM_CACHE,
            ':app:compileDebugShaders': FROM_CACHE,
            ':app:compileDebugSources': UP_TO_DATE,
            ':app:compileReleaseAidl': FROM_CACHE,
            ':app:compileReleaseJavaWithJavac': FROM_CACHE,
            ':app:compileReleaseNdk': NO_SOURCE,
            ':app:compileReleaseRenderscript': FROM_CACHE,
            ':app:compileReleaseShaders': FROM_CACHE,
            ':app:compileReleaseSources': UP_TO_DATE,
            ':app:createDebugCompatibleScreenManifests': FROM_CACHE,
            ':app:createReleaseCompatibleScreenManifests': FROM_CACHE,
            ':app:generateDebugAssets': UP_TO_DATE,
            ':app:generateDebugBuildConfig': FROM_CACHE,
            ':app:generateDebugResources': UP_TO_DATE,
            ':app:generateDebugResValues': FROM_CACHE,
            ':app:generateDebugSources': SUCCESS,
            ':app:generateReleaseAssets': UP_TO_DATE,
            ':app:generateReleaseBuildConfig': FROM_CACHE,
            ':app:generateReleaseResources': UP_TO_DATE,
            ':app:generateReleaseResValues': FROM_CACHE,
            ':app:generateReleaseSources': SUCCESS,
            ':app:javaPreCompileDebug': FROM_CACHE,
            ':app:javaPreCompileRelease': FROM_CACHE,
            ':app:lintVitalRelease': SUCCESS,
            ':app:mergeDebugAssets': FROM_CACHE,
            ':app:mergeDebugJniLibFolders': FROM_CACHE,
            ':app:mergeDebugResources': FROM_CACHE,
            ':app:mergeDebugShaders': FROM_CACHE,
            ':app:mergeReleaseAssets': FROM_CACHE,
            ':app:mergeReleaseJniLibFolders': FROM_CACHE,
            ':app:mergeReleaseResources': FROM_CACHE,
            ':app:mergeReleaseShaders': FROM_CACHE,
            ':app:packageDebug': SUCCESS,
            ':app:packageRelease': SUCCESS,
            ':app:preBuild': UP_TO_DATE,
            ':app:preDebugBuild': FROM_CACHE,
            ':app:prepareLintJar': SUCCESS,
            ':app:preReleaseBuild': FROM_CACHE,
            ':app:processDebugJavaRes': NO_SOURCE,
            ':app:processDebugManifest': FROM_CACHE,
            ':app:processDebugResources': FROM_CACHE,
            ':app:processReleaseJavaRes': NO_SOURCE,
            ':app:processReleaseManifest': FROM_CACHE,
            ':app:processReleaseResources': FROM_CACHE,
            ':app:splitsDiscoveryTaskDebug': FROM_CACHE,
            ':app:splitsDiscoveryTaskRelease': FROM_CACHE,
            ':app:transformClassesWithDexBuilderForDebug': SUCCESS,
            ':app:transformClassesWithPreDexForRelease': SUCCESS,
            // TODO For some reason this doesn't load from cache on Travis
            ':app:transformDexArchiveWithDexMergerForDebug': { GradleVersion gradleVersion ->
                if (Boolean.getBoolean("travis") && gradleVersion < GradleVersion.version("4.2")) {
                    SUCCESS
                } else {
                    FROM_CACHE
                }
            } as DynamicTaskOutcome,
            ':app:transformDexArchiveWithExternalLibsDexMergerForDebug': SUCCESS,
            ':app:transformDexWithDexForRelease': SUCCESS,
            ':app:transformNativeLibsWithMergeJniLibsForDebug': SUCCESS,
            ':app:transformNativeLibsWithMergeJniLibsForRelease': SUCCESS,
            ':app:transformResourcesWithMergeJavaResForDebug': SUCCESS,
            ':app:transformResourcesWithMergeJavaResForRelease': SUCCESS,
            ':app:validateSigningDebug': SUCCESS,
            ':library:assemble': SUCCESS,
            ':library:assembleDebug': SUCCESS,
            ':library:assembleRelease': SUCCESS,
            ':library:bundleDebug': SUCCESS,
            ':library:bundleRelease': SUCCESS,
            ':library:checkDebugManifest': FROM_CACHE,
            ':library:checkReleaseManifest': FROM_CACHE,
            ':library:compileDebugAidl': FROM_CACHE,
            ':library:compileDebugJavaWithJavac': FROM_CACHE,
            ':library:compileDebugNdk': NO_SOURCE,
            ':library:compileDebugRenderscript': FROM_CACHE,
            ':library:compileDebugShaders': FROM_CACHE,
            ':library:compileDebugSources': UP_TO_DATE,
            ':library:compileReleaseAidl': FROM_CACHE,
            ':library:compileReleaseJavaWithJavac': FROM_CACHE,
            ':library:compileReleaseNdk': NO_SOURCE,
            ':library:compileReleaseRenderscript': FROM_CACHE,
            ':library:compileReleaseShaders': FROM_CACHE,
            ':library:compileReleaseSources': UP_TO_DATE,
            ':library:extractDebugAnnotations': FROM_CACHE,
            ':library:extractReleaseAnnotations': SUCCESS,
            ':library:generateDebugAssets': UP_TO_DATE,
            ':library:generateDebugBuildConfig': FROM_CACHE,
            ':library:generateDebugResources': UP_TO_DATE,
            ':library:generateDebugResValues': FROM_CACHE,
            ':library:generateDebugSources': SUCCESS,
            ':library:generateReleaseAssets': UP_TO_DATE,
            ':library:generateReleaseBuildConfig': FROM_CACHE,
            ':library:generateReleaseResources': UP_TO_DATE,
            ':library:generateReleaseResValues': FROM_CACHE,
            ':library:generateReleaseSources': SUCCESS,
            ':library:javaPreCompileDebug': FROM_CACHE,
            ':library:javaPreCompileRelease': FROM_CACHE,
            ':library:mergeDebugAssets': FROM_CACHE,
            ':library:mergeDebugConsumerProguardFiles': SUCCESS,
            ':library:mergeDebugJniLibFolders': FROM_CACHE,
            ':library:mergeDebugShaders': FROM_CACHE,
            ':library:mergeReleaseAssets': FROM_CACHE,
            ':library:mergeReleaseConsumerProguardFiles': SUCCESS,
            ':library:mergeReleaseJniLibFolders': FROM_CACHE,
            // TODO This produces overlapping outputs in build/intermediates/typedefs.txt
            ':library:mergeReleaseResources': { GradleVersion gradleVersion -> gradleVersion < GradleVersion.version("4.2") ? FROM_CACHE : SUCCESS } as DynamicTaskOutcome,
            ':library:mergeReleaseShaders': FROM_CACHE,
            ':library:packageDebugRenderscript': NO_SOURCE,
            ':library:packageDebugResources': FROM_CACHE,
            ':library:packageReleaseRenderscript': NO_SOURCE,
            ':library:packageReleaseResources': FROM_CACHE,
            ':library:platformAttrExtractor': FROM_CACHE,
            ':library:preBuild': UP_TO_DATE,
            ':library:preDebugBuild': UP_TO_DATE,
            ':library:prepareLintJar': SUCCESS,
            ':library:preReleaseBuild': UP_TO_DATE,
            ':library:processDebugJavaRes': NO_SOURCE,
            ':library:processDebugManifest': FROM_CACHE,
            ':library:processDebugResources': FROM_CACHE,
            ':library:processReleaseJavaRes': NO_SOURCE,
            ':library:processReleaseManifest': FROM_CACHE,
            ':library:processReleaseResources': FROM_CACHE,
            ':library:transformClassesAndResourcesWithPrepareIntermediateJarsForDebug': SUCCESS,
            ':library:transformClassesAndResourcesWithPrepareIntermediateJarsForRelease': SUCCESS,
            ':library:transformClassesAndResourcesWithSyncLibJarsForDebug': SUCCESS,
            ':library:transformClassesAndResourcesWithSyncLibJarsForRelease': SUCCESS,
            ':library:transformNativeLibsWithIntermediateJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithIntermediateJniLibsForRelease': SUCCESS,
            ':library:transformNativeLibsWithMergeJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithMergeJniLibsForRelease': SUCCESS,
            ':library:transformNativeLibsWithSyncJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithSyncJniLibsForRelease': SUCCESS,
            ':library:transformResourcesWithMergeJavaResForDebug': SUCCESS,
            ':library:transformResourcesWithMergeJavaResForRelease': SUCCESS,
            ':library:verifyReleaseResources': SUCCESS,
        ]),

        (VersionNumber.parse("3.1.0-alpha01")): new ExpectedResults([
            ':app:assemble': SUCCESS,
            ':app:assembleDebug': SUCCESS,
            ':app:assembleRelease': SUCCESS,
            ':app:checkDebugManifest': FROM_CACHE,
            ':app:checkReleaseManifest': FROM_CACHE,
            ':app:compileDebugAidl': FROM_CACHE,
            ':app:compileDebugJavaWithJavac': FROM_CACHE,
            ':app:compileDebugNdk': NO_SOURCE,
            ':app:compileDebugRenderscript': FROM_CACHE,
            ':app:compileDebugShaders': FROM_CACHE,
            ':app:compileDebugSources': UP_TO_DATE,
            ':app:compileReleaseAidl': FROM_CACHE,
            ':app:compileReleaseJavaWithJavac': FROM_CACHE,
            ':app:compileReleaseNdk': NO_SOURCE,
            ':app:compileReleaseRenderscript': FROM_CACHE,
            ':app:compileReleaseShaders': FROM_CACHE,
            ':app:compileReleaseSources': UP_TO_DATE,
            ':app:createDebugCompatibleScreenManifests': FROM_CACHE,
            ':app:createReleaseCompatibleScreenManifests': FROM_CACHE,
            ':app:generateDebugAssets': UP_TO_DATE,
            ':app:generateDebugBuildConfig': FROM_CACHE,
            ':app:generateDebugResources': UP_TO_DATE,
            ':app:generateDebugResValues': FROM_CACHE,
            ':app:generateDebugSources': SUCCESS,
            ':app:generateReleaseAssets': UP_TO_DATE,
            ':app:generateReleaseBuildConfig': FROM_CACHE,
            ':app:generateReleaseResources': UP_TO_DATE,
            ':app:generateReleaseResValues': FROM_CACHE,
            ':app:generateReleaseSources': SUCCESS,
            ':app:javaPreCompileDebug': FROM_CACHE,
            ':app:javaPreCompileRelease': FROM_CACHE,
            ':app:lintVitalRelease': SUCCESS,
            ':app:mergeDebugAssets': FROM_CACHE,
            ':app:mergeDebugJniLibFolders': FROM_CACHE,
            ':app:mergeDebugResources': FROM_CACHE,
            ':app:mergeDebugShaders': FROM_CACHE,
            ':app:mergeReleaseAssets': FROM_CACHE,
            ':app:mergeReleaseJniLibFolders': FROM_CACHE,
            ':app:mergeReleaseResources': FROM_CACHE,
            ':app:mergeReleaseShaders': FROM_CACHE,
            ':app:packageDebug': SUCCESS,
            ':app:packageRelease': SUCCESS,
            ':app:preBuild': UP_TO_DATE,
            ':app:preDebugBuild': FROM_CACHE,
            ':app:prepareLintJar': SUCCESS,
            ':app:preReleaseBuild': FROM_CACHE,
            ':app:processDebugJavaRes': NO_SOURCE,
            ':app:processDebugManifest': FROM_CACHE,
            ':app:processDebugResources': FROM_CACHE,
            ':app:processReleaseJavaRes': NO_SOURCE,
            ':app:processReleaseManifest': FROM_CACHE,
            ':app:processReleaseResources': FROM_CACHE,
            ':app:splitsDiscoveryTaskDebug': FROM_CACHE,
            ':app:splitsDiscoveryTaskRelease': FROM_CACHE,
            ':app:transformClassesWithDexBuilderForDebug': SUCCESS,
            ':app:transformClassesWithDexBuilderForRelease': SUCCESS,
            ':app:transformDexArchiveWithDexMergerForDebug': SUCCESS,
            ':app:transformDexArchiveWithDexMergerForRelease': SUCCESS,
            ':app:transformDexArchiveWithExternalLibsDexMergerForDebug': SUCCESS,
            ':app:transformDexArchiveWithExternalLibsDexMergerForRelease': SUCCESS,
            ':app:transformNativeLibsWithMergeJniLibsForDebug': SUCCESS,
            ':app:transformNativeLibsWithMergeJniLibsForRelease': SUCCESS,
            ':app:transformResourcesWithMergeJavaResForDebug': SUCCESS,
            ':app:transformResourcesWithMergeJavaResForRelease': SUCCESS,
            ':app:validateSigningDebug': SUCCESS,
            ':library:assemble': SUCCESS,
            ':library:assembleDebug': SUCCESS,
            ':library:assembleRelease': SUCCESS,
            ':library:bundleDebug': SUCCESS,
            ':library:bundleRelease': SUCCESS,
            ':library:checkDebugManifest': FROM_CACHE,
            ':library:checkReleaseManifest': FROM_CACHE,
            ':library:compileDebugAidl': FROM_CACHE,
            ':library:compileDebugJavaWithJavac': FROM_CACHE,
            ':library:compileDebugNdk': NO_SOURCE,
            ':library:compileDebugRenderscript': FROM_CACHE,
            ':library:compileDebugShaders': FROM_CACHE,
            ':library:compileDebugSources': UP_TO_DATE,
            ':library:compileReleaseAidl': FROM_CACHE,
            ':library:compileReleaseJavaWithJavac': FROM_CACHE,
            ':library:compileReleaseNdk': NO_SOURCE,
            ':library:compileReleaseRenderscript': FROM_CACHE,
            ':library:compileReleaseShaders': FROM_CACHE,
            ':library:compileReleaseSources': UP_TO_DATE,
            ':library:extractDebugAnnotations': FROM_CACHE,
            ':library:extractReleaseAnnotations': SUCCESS,
            ':library:generateDebugAssets': UP_TO_DATE,
            ':library:generateDebugBuildConfig': FROM_CACHE,
            ':library:generateDebugResources': UP_TO_DATE,
            ':library:generateDebugResValues': FROM_CACHE,
            ':library:generateDebugSources': SUCCESS,
            ':library:generateReleaseAssets': UP_TO_DATE,
            ':library:generateReleaseBuildConfig': FROM_CACHE,
            ':library:generateReleaseResources': UP_TO_DATE,
            ':library:generateReleaseResValues': FROM_CACHE,
            ':library:generateReleaseSources': SUCCESS,
            ':library:javaPreCompileDebug': FROM_CACHE,
            ':library:javaPreCompileRelease': FROM_CACHE,
            ':library:mergeDebugConsumerProguardFiles': SUCCESS,
            ':library:mergeDebugJniLibFolders': FROM_CACHE,
            ':library:mergeDebugShaders': FROM_CACHE,
            ':library:mergeReleaseConsumerProguardFiles': SUCCESS,
            ':library:mergeReleaseJniLibFolders': FROM_CACHE,
            ':library:mergeReleaseResources': SUCCESS,
            ':library:mergeReleaseShaders': FROM_CACHE,
            ':library:packageDebugAssets': FROM_CACHE,
            ':library:packageDebugRenderscript': NO_SOURCE,
            ':library:packageDebugResources': FROM_CACHE,
            ':library:packageReleaseAssets': FROM_CACHE,
            ':library:packageReleaseRenderscript': NO_SOURCE,
            ':library:packageReleaseResources': FROM_CACHE,
            ':library:platformAttrExtractor': FROM_CACHE,
            ':library:preBuild': UP_TO_DATE,
            ':library:preDebugBuild': UP_TO_DATE,
            ':library:prepareLintJar': SUCCESS,
            ':library:preReleaseBuild': UP_TO_DATE,
            ':library:processDebugJavaRes': NO_SOURCE,
            ':library:processDebugManifest': FROM_CACHE,
            ':library:processDebugResources': FROM_CACHE,
            ':library:processReleaseJavaRes': NO_SOURCE,
            ':library:processReleaseManifest': FROM_CACHE,
            ':library:processReleaseResources': FROM_CACHE,
            ':library:transformClassesAndResourcesWithPrepareIntermediateJarsForDebug': SUCCESS,
            ':library:transformClassesAndResourcesWithPrepareIntermediateJarsForRelease': SUCCESS,
            ':library:transformClassesAndResourcesWithSyncLibJarsForDebug': SUCCESS,
            ':library:transformClassesAndResourcesWithSyncLibJarsForRelease': SUCCESS,
            ':library:transformNativeLibsWithIntermediateJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithIntermediateJniLibsForRelease': SUCCESS,
            ':library:transformNativeLibsWithMergeJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithMergeJniLibsForRelease': SUCCESS,
            ':library:transformNativeLibsWithSyncJniLibsForDebug': SUCCESS,
            ':library:transformNativeLibsWithSyncJniLibsForRelease': SUCCESS,
            ':library:transformResourcesWithMergeJavaResForDebug': SUCCESS,
            ':library:transformResourcesWithMergeJavaResForRelease': SUCCESS,
            ':library:verifyReleaseResources': SUCCESS,
        ]),
    ]
}
