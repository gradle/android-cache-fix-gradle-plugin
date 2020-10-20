package org.gradle.android

import com.google.common.collect.ImmutableMap
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

import static org.gradle.android.Versions.android
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class CrossVersionOutcomeAndRelocationTest extends AbstractTest {

    @Unroll
    def "simple Android app is relocatable with #gradleVersion and Android plugin #androidVersion"() {
        assert gradleVersion instanceof GradleVersion
        assert androidVersion instanceof VersionNumber

        println "> Using Android plugin $androidVersion"
        println "> Running with $gradleVersion"

        def originalDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(originalDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .build()
            .writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(relocatedDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .build()
            .writeProject()

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
                if (taskOutcome == null) {
                    println "> Task '$taskName' was expected to execute but did not"
                    allMatched = false
                } else if (expectedOutcome != ExpectedOutcome.UNKNOWN && taskOutcome.name() != expectedOutcome.name()) {
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

    private static class ExpectedOutcomeBuilder {
        private ImmutableMap.Builder<String, ExpectedOutcome> mapBuilder = new ImmutableMap.Builder<String, ExpectedOutcome>()

        ExpectedOutcomeBuilder expect(String key, TaskOutcome value) {
            mapBuilder.put(key, ExpectedOutcome.valueOf(value.name()))
            return this
        }

        ExpectedOutcomeBuilder expectChangingValue(String key) {
            mapBuilder.put(key, ExpectedOutcome.UNKNOWN)
            return this
        }

        ImmutableMap<String, ExpectedOutcome> build() {
            return mapBuilder.build()
        }
    }

    private static ExpectedResults expectedResults(VersionNumber androidVersion, GradleVersion gradleVersion) {
        def isAndroid35xOrHigher = androidVersion >= android("3.5.0")
        def isAndroid350to352 = androidVersion >= android("3.5.0") && androidVersion <= android("3.5.2")
        def isAndroid35x = androidVersion >= android("3.5.0") && androidVersion < android("3.6.0")
        def isAndroid35xTo36x = androidVersion >= android("3.5.0") && androidVersion <= android("3.6.4")
        def isAndroid35xTo40x = androidVersion >= android("3.5.0") && androidVersion <= android("4.1.0-alpha01")
        def isAndroid35xTo41x = androidVersion >= android("3.5.0") && androidVersion <= android("4.2.0-alpha01")
        def isAndroid36xOrHigher = androidVersion >= android("3.6.0")
        def isAndroid40xOrHigher = androidVersion >= android("4.0.0-beta01")
        def isAndroid40x = androidVersion >= android("4.0.0") && androidVersion < android("4.1.0-alpha01")
        def isAndroid40xTo41x = androidVersion >= android("4.0.0") && androidVersion <= android("4.2.0-alpha01")
        def isAndroid41xOrHigher = androidVersion >= android("4.1.0-alpha01")
        def isandroid41x = androidVersion >= android("4.1.0-alpha01") && androidVersion < android("4.2.0-alpha01")
        def isAndroid42xOrHigher = androidVersion >= android("4.2.0-alpha01")
        def builder = new ExpectedOutcomeBuilder()

        // Applies to anything 3.5.0 or higher
        if (isAndroid35xOrHigher) {
            android35xOrHigherExpectations(builder)
        }

        // Applies only to 3.5.x
        if (isAndroid35x) {
            android35xOnlyExpectations(builder)
        }

        // Applies to 3.5.0, 3.5.1, and 3.5.2
        if (isAndroid350to352) {
            android350to352OnlyExpectations(builder)
        }

        // Applies to 3.5.x or 3.6.x
        if (isAndroid35xTo36x) {
            android35xTo36xExpectations(builder)
        }

        // Applies to 3.5.x, 3.6.x, 4.0.x and 4.1.x
        if (isAndroid35xTo41x) {
            android35xTo41xExpectations(builder)
        }

        // Applies to anything 3.6.0 or higher
        if (isAndroid36xOrHigher) {
            android36xOrHigherExpectations(builder)
        }

        // Applies to anything 4.0.0 or higher
        if (isAndroid40xOrHigher) {
            android40xOrHigherExpectations(builder)
        }

        if (isAndroid35xTo40x) {
            android35xTo40xExpectations(builder)
        }

        if (isAndroid40x) {
            android40xOnlyExpectations(builder)
        }

        if (isAndroid40xTo41x) {
            android40xTo41xExpectation(builder)
        }

        // Applies to anything 4.1.0 or higher
        if (isAndroid41xOrHigher) {
            android41xOrHigherExpectations(builder)
        }

        if (isandroid41x) {
            android41xOnlyExpectations(builder)
        }

        if (isAndroid42xOrHigher) {
            android42xOrHigherExpectations(builder)
        }

        new ExpectedResults(
            builder.build()
        )
    }

    static void android35xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:assemble', SUCCESS)
        builder.expect(':app:assembleDebug', SUCCESS)
        builder.expect(':app:assembleRelease', SUCCESS)
        builder.expect(':app:checkDebugDuplicateClasses', FROM_CACHE)
        builder.expect(':app:checkReleaseDuplicateClasses', FROM_CACHE)
        builder.expect(':app:compileDebugAidl', NO_SOURCE)
        builder.expect(':app:compileDebugJavaWithJavac', FROM_CACHE)
        builder.expect(':app:compileDebugKotlin', FROM_CACHE)
        builder.expect(':app:compileDebugRenderscript', FROM_CACHE)
        builder.expect(':app:compileDebugSources', UP_TO_DATE)
        builder.expect(':app:compileReleaseAidl', NO_SOURCE)
        builder.expect(':app:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.expect(':app:compileReleaseKotlin', FROM_CACHE)
        builder.expect(':app:compileReleaseRenderscript', FROM_CACHE)
        builder.expect(':app:compileReleaseSources', UP_TO_DATE)
        builder.expect(':app:createDebugCompatibleScreenManifests', FROM_CACHE)
        builder.expect(':app:createReleaseCompatibleScreenManifests', FROM_CACHE)
        builder.expect(':app:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.expect(':app:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.expect(':app:dataBindingMergeDependencyArtifactsDebug', FROM_CACHE)
        builder.expect(':app:dataBindingMergeDependencyArtifactsRelease', FROM_CACHE)
        builder.expect(':app:generateDebugAssets', UP_TO_DATE)
        builder.expect(':app:generateDebugBuildConfig', FROM_CACHE)
        builder.expect(':app:generateDebugResValues', FROM_CACHE)
        builder.expect(':app:generateDebugResources', UP_TO_DATE)
        builder.expect(':app:generateReleaseAssets', UP_TO_DATE)
        builder.expect(':app:generateReleaseBuildConfig', FROM_CACHE)
        builder.expect(':app:generateReleaseResValues', FROM_CACHE)
        builder.expect(':app:generateReleaseResources', UP_TO_DATE)
        builder.expect(':app:kaptGenerateStubsDebugKotlin', FROM_CACHE)
        builder.expect(':app:kaptDebugKotlin', FROM_CACHE)
        builder.expect(':app:kaptGenerateStubsReleaseKotlin', FROM_CACHE)
        builder.expect(':app:kaptReleaseKotlin', FROM_CACHE)
        builder.expect(':app:lintVitalRelease', SUCCESS)
        builder.expect(':app:mergeDebugResources', FROM_CACHE)
        builder.expect(':app:mergeReleaseResources', FROM_CACHE)
        builder.expect(':app:processDebugResources', FROM_CACHE)
        builder.expect(':app:processReleaseResources', FROM_CACHE)
        builder.expect(':app:mergeDebugAssets', FROM_CACHE)
        builder.expect(':app:mergeDebugJavaResource', FROM_CACHE)
        builder.expect(':app:mergeDebugJniLibFolders', FROM_CACHE)
        builder.expect(':app:mergeDebugShaders', FROM_CACHE)
        builder.expect(':app:mergeDexRelease', FROM_CACHE)
        builder.expect(':app:mergeExtDexDebug', FROM_CACHE)
        builder.expect(':app:mergeExtDexRelease', FROM_CACHE)
        builder.expect(':app:mergeLibDexDebug', FROM_CACHE)
        builder.expect(':app:mergeProjectDexDebug', FROM_CACHE)
        builder.expect(':app:mergeReleaseAssets', FROM_CACHE)
        builder.expect(':app:mergeReleaseJavaResource', FROM_CACHE)
        builder.expect(':app:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.expect(':app:mergeReleaseShaders', FROM_CACHE)
        builder.expect(':app:mergeRoomSchemaLocations', SUCCESS)
        builder.expect(':app:packageDebug', SUCCESS)
        builder.expect(':app:packageRelease', SUCCESS)
        builder.expect(':app:preBuild', UP_TO_DATE)
        builder.expect(':app:preDebugBuild', UP_TO_DATE)
        builder.expect(':app:preReleaseBuild', UP_TO_DATE)
        builder.expect(':app:processDebugJavaRes', NO_SOURCE)
        builder.expect(':app:processDebugManifest', FROM_CACHE)
        builder.expect(':app:processReleaseJavaRes', NO_SOURCE)
        builder.expect(':app:processReleaseManifest', FROM_CACHE)
        builder.expect(':app:stripDebugDebugSymbols', FROM_CACHE)
        builder.expect(':app:stripReleaseDebugSymbols', FROM_CACHE)
        builder.expect(':app:validateSigningDebug', FROM_CACHE)
        builder.expect(':library:assemble', SUCCESS)
        builder.expect(':library:assembleDebug', SUCCESS)
        builder.expect(':library:assembleRelease', SUCCESS)
        builder.expect(':library:bundleDebugAar', SUCCESS)
        builder.expect(':library:bundleReleaseAar', SUCCESS)
        builder.expect(':library:compileDebugAidl', NO_SOURCE)
        builder.expect(':library:compileDebugJavaWithJavac', FROM_CACHE)
        builder.expect(':library:compileDebugKotlin', FROM_CACHE)
        builder.expect(':library:compileDebugRenderscript', FROM_CACHE)
        builder.expect(':library:compileDebugSources', UP_TO_DATE)
        builder.expect(':library:compileReleaseAidl', NO_SOURCE)
        builder.expect(':library:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.expect(':library:compileReleaseKotlin', FROM_CACHE)
        builder.expect(':library:compileReleaseRenderscript', FROM_CACHE)
        builder.expect(':library:compileReleaseSources', UP_TO_DATE)
        builder.expect(':library:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.expect(':library:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.expect(':library:dataBindingMergeDependencyArtifactsDebug', FROM_CACHE)
        builder.expect(':library:dataBindingMergeDependencyArtifactsRelease', FROM_CACHE)
        builder.expect(':library:extractDebugAnnotations', FROM_CACHE)
        builder.expect(':library:extractReleaseAnnotations', FROM_CACHE)
        builder.expect(':library:generateDebugAssets', UP_TO_DATE)
        builder.expect(':library:generateDebugBuildConfig', FROM_CACHE)
        builder.expect(':library:generateDebugRFile', FROM_CACHE)
        builder.expect(':library:generateDebugResValues', FROM_CACHE)
        builder.expect(':library:generateDebugResources', UP_TO_DATE)
        builder.expect(':library:generateReleaseAssets', UP_TO_DATE)
        builder.expect(':library:generateReleaseBuildConfig', FROM_CACHE)
        builder.expect(':library:generateReleaseRFile', FROM_CACHE)
        builder.expect(':library:generateReleaseResValues', FROM_CACHE)
        builder.expect(':library:generateReleaseResources', UP_TO_DATE)
        builder.expect(':library:kaptGenerateStubsDebugKotlin', FROM_CACHE)
        builder.expect(':library:kaptDebugKotlin', FROM_CACHE)
        builder.expect(':library:kaptGenerateStubsReleaseKotlin', FROM_CACHE)
        builder.expect(':library:kaptReleaseKotlin', FROM_CACHE)
        builder.expect(':library:mergeDebugJavaResource', FROM_CACHE)
        builder.expect(':library:mergeDebugJniLibFolders', FROM_CACHE)
        builder.expect(':library:mergeDebugShaders', FROM_CACHE)
        builder.expect(':library:mergeReleaseJavaResource', FROM_CACHE)
        builder.expect(':library:mergeReleaseJniLibFolders', FROM_CACHE)
        builder.expect(':library:mergeReleaseResources', FROM_CACHE)
        builder.expect(':library:mergeReleaseShaders', FROM_CACHE)
        builder.expect(':library:packageDebugAssets', FROM_CACHE)
        builder.expect(':library:packageDebugRenderscript', NO_SOURCE)
        builder.expect(':library:packageDebugResources', FROM_CACHE)
        builder.expect(':library:packageReleaseAssets', FROM_CACHE)
        builder.expect(':library:packageReleaseRenderscript', NO_SOURCE)
        builder.expect(':library:packageReleaseResources', FROM_CACHE)
        builder.expect(':library:mergeRoomSchemaLocations', SUCCESS)
        builder.expect(':library:preBuild', UP_TO_DATE)
        builder.expect(':library:preDebugBuild', UP_TO_DATE)
        builder.expect(':library:preReleaseBuild', UP_TO_DATE)
        builder.expect(':library:prepareLintJarForPublish', SUCCESS)
        builder.expect(':library:processDebugJavaRes', NO_SOURCE)
        builder.expect(':library:processDebugManifest', FROM_CACHE)
        builder.expect(':library:processReleaseJavaRes', NO_SOURCE)
        builder.expect(':library:processReleaseManifest', FROM_CACHE)
        builder.expect(':library:stripDebugDebugSymbols', FROM_CACHE)
        builder.expect(':library:stripReleaseDebugSymbols', FROM_CACHE)

        // the outcome of this task is not consistent with the kotlin plugin applied
        builder.expectChangingValue(':library:verifyReleaseResources')
    }

    static void android35xTo36xExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:compileDebugShaders', FROM_CACHE)
        builder.expect(':app:compileReleaseShaders', FROM_CACHE)
        builder.expect(':app:dataBindingExportBuildInfoDebug', SUCCESS)
        builder.expect(':app:dataBindingExportBuildInfoRelease', SUCCESS)
        builder.expect(':app:dataBindingMergeGenClassesDebug', SUCCESS)
        builder.expect(':app:dataBindingMergeGenClassesRelease', SUCCESS)
        builder.expect(':app:mainApkListPersistenceDebug', FROM_CACHE)
        builder.expect(':app:mainApkListPersistenceRelease', FROM_CACHE)
        builder.expect(':library:bundleLibCompileDebug', SUCCESS)
        builder.expect(':library:bundleLibCompileRelease', SUCCESS)
        builder.expect(':library:bundleLibResDebug', SUCCESS)
        builder.expect(':library:bundleLibResRelease', SUCCESS)
        builder.expect(':library:bundleLibRuntimeDebug', SUCCESS)
        builder.expect(':library:bundleLibRuntimeRelease', SUCCESS)
        builder.expect(':library:compileDebugShaders', FROM_CACHE)
        builder.expect(':library:compileReleaseShaders', FROM_CACHE)
        builder.expect(':library:dataBindingExportBuildInfoDebug', SUCCESS)
        builder.expect(':library:dataBindingExportBuildInfoRelease', SUCCESS)
        builder.expect(':library:dataBindingMergeGenClassesDebug', SUCCESS)
        builder.expect(':library:dataBindingMergeGenClassesRelease', SUCCESS)
        builder.expect(':library:mergeDebugConsumerProguardFiles', SUCCESS)
        builder.expect(':library:mergeDebugGeneratedProguardFiles', SUCCESS)
        builder.expect(':library:mergeReleaseConsumerProguardFiles', SUCCESS)
        builder.expect(':library:mergeReleaseGeneratedProguardFiles', SUCCESS)
    }

    static void android35xTo40xExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:prepareLintJarForPublish', SUCCESS)
        builder.expect(':app:dataBindingExportFeaturePackageIdsDebug', FROM_CACHE)
        builder.expect(':app:dataBindingExportFeaturePackageIdsRelease', FROM_CACHE)
        builder.expect(':app:generateDebugSources', SUCCESS)
        builder.expect(':app:generateReleaseSources', SUCCESS)
        builder.expect(':library:generateDebugSources', SUCCESS)
        builder.expect(':library:generateReleaseSources', SUCCESS)
        builder.expect(':library:prepareLintJar', SUCCESS)
        builder.expect(':app:prepareLintJar', SUCCESS)
    }

    static void android35xTo41xExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:javaPreCompileDebug', FROM_CACHE)
        builder.expect(':app:javaPreCompileRelease', FROM_CACHE)
        builder.expect(':library:javaPreCompileDebug', FROM_CACHE)
        builder.expect(':library:javaPreCompileRelease', FROM_CACHE)
    }


    static void android35xOnlyExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:checkDebugManifest', SUCCESS)
        builder.expect(':app:checkReleaseManifest', SUCCESS)
        builder.expect(':app:mergeDebugNativeLibs', SUCCESS)
        builder.expect(':app:mergeReleaseNativeLibs', SUCCESS)
        builder.expect(':app:signingConfigWriterDebug', FROM_CACHE)
        builder.expect(':app:signingConfigWriterRelease', FROM_CACHE)
        builder.expect(':app:transformClassesWithDexBuilderForDebug', SUCCESS)
        builder.expect(':app:transformClassesWithDexBuilderForRelease', SUCCESS)
        builder.expect(':library:checkDebugManifest', SUCCESS)
        builder.expect(':library:checkReleaseManifest', SUCCESS)
        builder.expect(':library:mergeDebugNativeLibs', SUCCESS)
        builder.expect(':library:mergeReleaseNativeLibs', SUCCESS)
        builder.expect(':library:parseDebugLibraryResources', FROM_CACHE)
        builder.expect(':library:parseReleaseLibraryResources', FROM_CACHE)
        builder.expect(':library:transformClassesAndResourcesWithSyncLibJarsForDebug', SUCCESS)
        builder.expect(':library:transformClassesAndResourcesWithSyncLibJarsForRelease', SUCCESS)
        builder.expect(':library:transformNativeLibsWithIntermediateJniLibsForDebug', SUCCESS)
        builder.expect(':library:transformNativeLibsWithIntermediateJniLibsForRelease', SUCCESS)
        builder.expect(':library:transformNativeLibsWithSyncJniLibsForDebug', SUCCESS)
        builder.expect(':library:transformNativeLibsWithSyncJniLibsForRelease', SUCCESS)
    }

    static void android350to352OnlyExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':library:createFullJarDebug', FROM_CACHE)
        builder.expect(':library:createFullJarRelease', FROM_CACHE)
    }

    static void android36xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:dexBuilderDebug', FROM_CACHE)
        builder.expect(':app:dexBuilderRelease', FROM_CACHE)
        builder.expect(':app:extractDeepLinksDebug', FROM_CACHE)
        builder.expect(':app:extractDeepLinksRelease', FROM_CACHE)
        builder.expect(':app:mergeDebugNativeLibs', FROM_CACHE)
        builder.expect(':app:mergeReleaseNativeLibs', FROM_CACHE)
        builder.expect(':library:copyDebugJniLibsProjectAndLocalJars', FROM_CACHE)
        builder.expect(':library:copyDebugJniLibsProjectOnly', FROM_CACHE)
        builder.expect(':library:copyReleaseJniLibsProjectAndLocalJars', FROM_CACHE)
        builder.expect(':library:copyReleaseJniLibsProjectOnly', FROM_CACHE)
        builder.expect(':library:extractDeepLinksDebug', FROM_CACHE)
        builder.expect(':library:extractDeepLinksRelease', FROM_CACHE)
        builder.expect(':library:mergeDebugNativeLibs', FROM_CACHE)
        builder.expect(':library:mergeReleaseNativeLibs', FROM_CACHE)
        builder.expect(':library:parseDebugLocalResources', FROM_CACHE)
        builder.expect(':library:parseReleaseLocalResources', FROM_CACHE)
        builder.expect(':library:syncDebugLibJars', FROM_CACHE)
        builder.expect(':library:syncReleaseLibJars', FROM_CACHE)
        builder.expect(':library:compileDebugLibraryResources', FROM_CACHE)
        builder.expect(':library:compileReleaseLibraryResources', FROM_CACHE)
    }

    static void android40xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:compileDebugShaders', NO_SOURCE)
        builder.expect(':app:compileReleaseShaders', NO_SOURCE)
        builder.expect(':app:dataBindingMergeGenClassesDebug', FROM_CACHE)
        builder.expect(':app:dataBindingMergeGenClassesRelease', FROM_CACHE)
        builder.expect(':library:compileDebugShaders', NO_SOURCE)
        builder.expect(':library:compileReleaseShaders', NO_SOURCE)
        builder.expect(':library:dataBindingMergeGenClassesDebug', FROM_CACHE)
        builder.expect(':library:dataBindingMergeGenClassesRelease', FROM_CACHE)
        builder.expect(':library:mergeDebugConsumerProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeDebugGeneratedProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeReleaseConsumerProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeReleaseGeneratedProguardFiles', FROM_CACHE)
        builder.expect(':library:bundleLibCompileToJarDebug', FROM_CACHE)
        builder.expect(':library:bundleLibResDebug', NO_SOURCE)
        builder.expect(':library:bundleLibResRelease', NO_SOURCE)
        builder.expect(':library:bundleLibCompileToJarRelease', FROM_CACHE)
        builder.expect(':app:collectReleaseDependencies', SUCCESS)
        builder.expect(':app:sdkReleaseDependencyData', SUCCESS)
    }

    static void android40xTo41xExpectation(ExpectedOutcomeBuilder builder) {
        builder.expect(':library:bundleLibRuntimeToJarDebug', FROM_CACHE)
        builder.expect(':library:bundleLibRuntimeToJarRelease', FROM_CACHE)
    }

    static void android41xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':library:dataBindingTriggerDebug', FROM_CACHE)
        builder.expect(':library:dataBindingTriggerRelease', FROM_CACHE)
        builder.expect(':app:dataBindingTriggerDebug', FROM_CACHE)
        builder.expect(':app:processDebugMainManifest', FROM_CACHE)
        builder.expect(':app:processDebugManifestForPackage', FROM_CACHE)
        builder.expect(':app:dataBindingTriggerRelease', FROM_CACHE)
        builder.expect(':app:processReleaseMainManifest', FROM_CACHE)
        builder.expect(':app:processReleaseManifestForPackage', FROM_CACHE)
        builder.expect(':app:compressDebugAssets', FROM_CACHE)
        builder.expect(':app:compressReleaseAssets', FROM_CACHE)
        builder.expect(':app:mergeDebugNativeDebugMetadata', NO_SOURCE)
        builder.expect(':app:checkDebugAarMetadata', FROM_CACHE)
        builder.expect(':app:checkReleaseAarMetadata', FROM_CACHE)
        builder.expect(':library:writeDebugAarMetadata', FROM_CACHE)
        builder.expect(':library:writeReleaseAarMetadata', FROM_CACHE)
    }

    static void android41xOnlyExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:mergeReleaseNativeDebugMetadata', NO_SOURCE)
    }

    static void android40xOnlyExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:dataBindingExportBuildInfoDebug', FROM_CACHE)
        builder.expect(':app:dataBindingExportBuildInfoRelease', FROM_CACHE)
        builder.expect(':library:dataBindingExportBuildInfoDebug', FROM_CACHE)
        builder.expect(':library:dataBindingExportBuildInfoRelease', FROM_CACHE)
    }

    static void android42xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:desugarDebugFileDependencies', FROM_CACHE)
        builder.expect(':app:desugarReleaseFileDependencies', FROM_CACHE)
        // Renamed from ToJar to ToDir
        builder.expect(':library:bundleLibRuntimeToDirDebug', FROM_CACHE)
        builder.expect(':library:bundleLibRuntimeToDirRelease', FROM_CACHE)
        builder.expect(':app:optimizeReleaseResources', FROM_CACHE)
        builder.expect(':app:mergeReleaseNativeDebugMetadata', FROM_CACHE)
        builder.expect(':app:writeDebugAppMetadata', FROM_CACHE)
        builder.expect(':app:extractReleaseNativeSymbolTables', FROM_CACHE)
        builder.expect(':app:writeReleaseAppMetadata', FROM_CACHE)
        // New non-cacheable tasks in 4.2.0-alpha10:
        builder.expect(':app:writeReleaseApplicationId', SUCCESS)
        builder.expect(':app:analyticsRecordingRelease', SUCCESS)
        builder.expect(':app:writeDebugSigningConfigVersions', FROM_CACHE)
        builder.expect(':app:writeReleaseSigningConfigVersions', FROM_CACHE)
    }
}
