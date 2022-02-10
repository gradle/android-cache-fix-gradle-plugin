package org.gradle.android

import com.google.common.collect.ImmutableMap
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber
import spock.lang.Unroll

import static org.gradle.android.TestVersions.latestKotlinVersionForGradleVersion
import static org.gradle.android.Versions.android
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

@MultiVersionTest
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
            .withKotlinVersion(latestKotlinVersionForGradleVersion(gradleVersion))
            .build()
            .writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        SimpleAndroidApp.builder(relocatedDir, cacheDir)
            .withAndroidVersion(androidVersion)
            .withKotlinVersion(latestKotlinVersionForGradleVersion(gradleVersion))
            .build()
            .writeProject()

        def expectedResults = expectedResults(androidVersion, latestKotlinVersionForGradleVersion(gradleVersion))

        println expectedResults.describe()

        cacheDir.deleteDir()
        cacheDir.mkdirs()

        withGradleVersion(gradleVersion.version)
            .withProjectDir(originalDir)
            .withArguments("assemble", "createFullJarDebug", "createFullJarRelease", "--build-cache", "--stacktrace")
            .build()

        when:
        def result = withGradleVersion(gradleVersion.version)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "createFullJarDebug", "createFullJarRelease", "--build-cache", "--stacktrace")
            .build()

        then:
        expectedResults.verify(result)

        cleanup:
        originalDir.deleteDir()
        relocatedDir.deleteDir()

        where:
        //noinspection GroovyAssignabilityCheck
        [androidVersion, gradleVersion] << TestVersions.allCandidateTestVersions.entries().collect { [it.key, it.value] }
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
        private Set<String> seen = []
        private ImmutableMap.Builder<String, ExpectedOutcome> mapBuilder = new ImmutableMap.Builder<String, ExpectedOutcome>()

        private void checkIfSeen(String key) {
            if (seen.contains(key)) {
                throw new IllegalArgumentException("The task ${key} already has an expected value!")
            } else {
                seen.add(key)
            }
        }

        ExpectedOutcomeBuilder expect(String key, TaskOutcome value) {
            checkIfSeen(key)
            mapBuilder.put(key, ExpectedOutcome.valueOf(value.name()))
            return this
        }

        ExpectedOutcomeBuilder expectChangingValue(String key) {
            checkIfSeen(key)
            mapBuilder.put(key, ExpectedOutcome.UNKNOWN)
            return this
        }

        ImmutableMap<String, ExpectedOutcome> build() {
            return mapBuilder.build()
        }
    }

    private static ExpectedResults expectedResults(VersionNumber androidVersion, VersionNumber kotlinVersion) {
        def isAndroid70xOrHigher = androidVersion >= android("7.0.0-alpha01")
        def isAndroid70xTo71x = androidVersion >= android("7.0.0-alpha01") && androidVersion < android("7.2.0-alpha01")
        def isAndroid70xOnly = androidVersion >= android("7.0.0-alpha01") && androidVersion < android("7.1.0-alpha01")
        def isAndroid71xOrHigher = androidVersion >= android("7.1.0-alpha01")
        def isAndroid71x = androidVersion >= android("7.1.0-alpha01") && androidVersion < android("7.2.0-alpha01")
        def isAndroid72xOrHigher = androidVersion >= android("7.2.0-alpha01")

        def builder = new ExpectedOutcomeBuilder()

        // Applies to anything 7.0.0-alpha01 or higher
        if (isAndroid70xOrHigher) {
            android35xOrHigherExpectations(builder)
        }

        if (isAndroid70xOnly) {
            android70xOnlyExpectations(builder)
        }

        if (isAndroid70xTo71x) {
            android70xTo71xExpectations(builder)
        }

        if (isAndroid71xOrHigher) {
            android71xOrHigherExpectations(builder)
        }

        if (isAndroid71x) {
            android71xOnlyExpectations(builder)
        }

        if (isAndroid72xOrHigher) {
            android72xOrHigherExpectations(builder)
        }

        if (kotlinVersion >= VersionNumber.parse("1.6.0")) {
            builder.expect(":app:buildKotlinToolingMetadata", SUCCESS)
        }

        new ExpectedResults(
            builder.build()
        )
    }

    static void android35xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:assemble', SUCCESS)
        builder.expect(':app:assembleDebug', SUCCESS)
        builder.expect(':app:assembleRelease', SUCCESS)
        builder.expect(':app:compileDebugAidl', NO_SOURCE)
        builder.expect(':app:compileDebugJavaWithJavac', FROM_CACHE)
        builder.expect(':app:compileDebugKotlin', FROM_CACHE)
        builder.expect(':app:compileDebugRenderscript', FROM_CACHE)
        builder.expect(':app:compileReleaseAidl', NO_SOURCE)
        builder.expect(':app:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.expect(':app:compileReleaseKotlin', FROM_CACHE)
        builder.expect(':app:compileReleaseRenderscript', FROM_CACHE)
        builder.expect(':app:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.expect(':app:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.expect(':app:dataBindingMergeDependencyArtifactsDebug', SUCCESS)
        builder.expect(':app:dataBindingMergeDependencyArtifactsRelease', SUCCESS)
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
        builder.expect(':app:mergeDebugAssets', SUCCESS)
        builder.expect(':app:mergeDebugJavaResource', SUCCESS)
        builder.expect(':app:mergeDebugJniLibFolders', SUCCESS)
        builder.expect(':app:mergeDebugShaders', SUCCESS)
        builder.expect(':app:mergeDexRelease', FROM_CACHE)
        builder.expect(':app:mergeExtDexDebug', FROM_CACHE)
        builder.expect(':app:mergeExtDexRelease', FROM_CACHE)
        builder.expect(':app:mergeLibDexDebug', FROM_CACHE)
        builder.expect(':app:mergeProjectDexDebug', FROM_CACHE)
        builder.expect(':app:mergeReleaseAssets', SUCCESS)
        builder.expect(':app:mergeReleaseJavaResource', SUCCESS)
        builder.expect(':app:mergeReleaseJniLibFolders', SUCCESS)
        builder.expect(':app:mergeReleaseShaders', SUCCESS)
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
        builder.expect(':library:assemble', SUCCESS)
        builder.expect(':library:assembleDebug', SUCCESS)
        builder.expect(':library:assembleRelease', SUCCESS)
        builder.expect(':library:bundleDebugAar', SUCCESS)
        builder.expect(':library:bundleReleaseAar', SUCCESS)
        builder.expect(':library:compileDebugAidl', NO_SOURCE)
        builder.expect(':library:compileDebugJavaWithJavac', FROM_CACHE)
        builder.expect(':library:compileDebugKotlin', FROM_CACHE)
        builder.expect(':library:compileDebugRenderscript', FROM_CACHE)
        builder.expect(':library:compileReleaseAidl', NO_SOURCE)
        builder.expect(':library:compileReleaseJavaWithJavac', FROM_CACHE)
        builder.expect(':library:compileReleaseKotlin', FROM_CACHE)
        builder.expect(':library:compileReleaseRenderscript', FROM_CACHE)
        builder.expect(':library:createFullJarDebug', SUCCESS)
        builder.expect(':library:createFullJarRelease', SUCCESS)
        builder.expect(':library:dataBindingGenBaseClassesDebug', FROM_CACHE)
        builder.expect(':library:dataBindingGenBaseClassesRelease', FROM_CACHE)
        builder.expect(':library:dataBindingMergeDependencyArtifactsDebug', SUCCESS)
        builder.expect(':library:dataBindingMergeDependencyArtifactsRelease', SUCCESS)
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
        builder.expect(':library:mergeDebugJavaResource', SUCCESS)
        builder.expect(':library:mergeDebugJniLibFolders', SUCCESS)
        builder.expect(':library:mergeDebugShaders', SUCCESS)
        builder.expect(':library:mergeReleaseJavaResource', SUCCESS)
        builder.expect(':library:mergeReleaseJniLibFolders', SUCCESS)
        builder.expect(':library:mergeReleaseResources', FROM_CACHE)
        builder.expect(':library:mergeReleaseShaders', SUCCESS)
        builder.expect(':library:packageDebugAssets', SUCCESS)
        builder.expect(':library:packageDebugRenderscript', NO_SOURCE)
        builder.expect(':library:packageDebugResources', FROM_CACHE)
        builder.expect(':library:packageReleaseAssets', SUCCESS)
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

        // the outcome of this task is not consistent with the kotlin plugin applied
        builder.expectChangingValue(':library:verifyReleaseResources')

        builder.expect(':app:processDebugMainManifest', FROM_CACHE)
        builder.expect(':app:processDebugManifestForPackage', FROM_CACHE)
        builder.expect(':app:processReleaseMainManifest', FROM_CACHE)
        builder.expect(':app:processReleaseManifestForPackage', FROM_CACHE)
        builder.expect(':app:compressDebugAssets', FROM_CACHE)
        builder.expect(':app:compressReleaseAssets', FROM_CACHE)
        builder.expect(':app:mergeDebugNativeDebugMetadata', NO_SOURCE)
        builder.expect(':library:mergeDebugNativeLibs', NO_SOURCE)
        builder.expect(':library:mergeReleaseNativeLibs', NO_SOURCE)

        builder.expect(':app:dexBuilderDebug', FROM_CACHE)
        builder.expect(':app:dexBuilderRelease', FROM_CACHE)
        builder.expect(':app:extractDeepLinksDebug', FROM_CACHE)
        builder.expect(':app:extractDeepLinksRelease', FROM_CACHE)
        builder.expect(':library:copyDebugJniLibsProjectAndLocalJars', SUCCESS)
        builder.expect(':library:copyDebugJniLibsProjectOnly', SUCCESS)
        builder.expect(':library:copyReleaseJniLibsProjectAndLocalJars', SUCCESS)
        builder.expect(':library:copyReleaseJniLibsProjectOnly', SUCCESS)
        builder.expect(':library:extractDeepLinksDebug', FROM_CACHE)
        builder.expect(':library:extractDeepLinksRelease', FROM_CACHE)
        builder.expect(':library:parseDebugLocalResources', FROM_CACHE)
        builder.expect(':library:parseReleaseLocalResources', FROM_CACHE)
        builder.expect(':library:syncDebugLibJars', FROM_CACHE)
        builder.expect(':library:syncReleaseLibJars', FROM_CACHE)
        builder.expect(':library:compileDebugLibraryResources', FROM_CACHE)
        builder.expect(':library:compileReleaseLibraryResources', FROM_CACHE)

        builder.expect(':app:compileDebugShaders', NO_SOURCE)
        builder.expect(':app:compileReleaseShaders', NO_SOURCE)
        builder.expect(':app:dataBindingMergeGenClassesDebug', FROM_CACHE)
        builder.expect(':app:dataBindingMergeGenClassesRelease', FROM_CACHE)
        builder.expect(':app:stripDebugDebugSymbols', NO_SOURCE)
        builder.expect(':app:stripReleaseDebugSymbols', NO_SOURCE)
        builder.expect(':library:compileDebugShaders', NO_SOURCE)
        builder.expect(':library:compileReleaseShaders', NO_SOURCE)
        builder.expect(':library:dataBindingMergeGenClassesDebug', FROM_CACHE)
        builder.expect(':library:dataBindingMergeGenClassesRelease', FROM_CACHE)
        builder.expect(':library:bundleLibCompileToJarDebug', SUCCESS)
        builder.expect(':library:bundleLibCompileToJarRelease', SUCCESS)
        builder.expect(':library:stripDebugDebugSymbols', NO_SOURCE)
        builder.expect(':library:stripReleaseDebugSymbols', NO_SOURCE)
        builder.expect(':app:collectReleaseDependencies', SUCCESS)
        builder.expect(':app:sdkReleaseDependencyData', SUCCESS)

        builder.expect(':library:bundleLibRuntimeToDirDebug', SUCCESS)
        builder.expect(':library:bundleLibRuntimeToDirRelease', SUCCESS)
        builder.expect(':library:bundleLibRuntimeToJarDebug', SUCCESS)
        builder.expect(':library:bundleLibRuntimeToJarRelease', SUCCESS)
        builder.expect(':app:optimizeReleaseResources', FROM_CACHE)
        builder.expect(':app:mergeReleaseNativeDebugMetadata', NO_SOURCE)

        builder.expect(':app:desugarDebugFileDependencies', FROM_CACHE)
        builder.expect(':app:desugarReleaseFileDependencies', FROM_CACHE)
        builder.expect(':app:extractReleaseNativeSymbolTables', NO_SOURCE)
        builder.expect(':app:mapDebugSourceSetPaths', SUCCESS)
        builder.expect(':app:mapReleaseSourceSetPaths', SUCCESS)
        builder.expect(':app:mergeDebugNativeLibs', NO_SOURCE)
        builder.expect(':app:mergeReleaseNativeLibs', NO_SOURCE)
        builder.expect(':app:mergeDebugResources', SUCCESS)
        builder.expect(':app:mergeReleaseResources', SUCCESS)
        builder.expect(':app:processDebugResources', FROM_CACHE)
        builder.expect(':app:processReleaseResources', FROM_CACHE)
        // New tasks in 7.0.0-beta04
        builder.expect(':library:javaPreCompileDebug', FROM_CACHE)
        builder.expect(':library:javaPreCompileRelease', FROM_CACHE)
        builder.expect(':library:mapReleaseSourceSetPaths', SUCCESS)
        builder.expect(':app:javaPreCompileDebug', FROM_CACHE)
        builder.expect(':app:javaPreCompileRelease', FROM_CACHE)
        // New non-cacheable tasks in 7.0.0-beta05
        builder.expect(':app:mergeReleaseArtProfile', SUCCESS)
        builder.expect(':app:compileReleaseArtProfile', FROM_CACHE)
        builder.expect(':library:prepareReleaseArtProfile', SUCCESS)
        builder.expect(':library:prepareDebugArtProfile', SUCCESS)
    }

    static void android70xOnlyExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:compileDebugSources', UP_TO_DATE)
        builder.expect(':app:compileReleaseSources', UP_TO_DATE)
        builder.expect(':library:compileDebugSources', UP_TO_DATE)
        builder.expect(':library:compileReleaseSources', UP_TO_DATE)
        builder.expect(':library:bundleLibResDebug', NO_SOURCE)
        builder.expect(':library:bundleLibResRelease', NO_SOURCE)
    }

    static void android70xTo71xExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:checkDebugDuplicateClasses', FROM_CACHE)
        builder.expect(':app:checkReleaseDuplicateClasses', FROM_CACHE)
        builder.expect(':app:createDebugCompatibleScreenManifests', FROM_CACHE)
        builder.expect(':app:createReleaseCompatibleScreenManifests', FROM_CACHE)
        builder.expect(':app:validateSigningDebug', FROM_CACHE)
        builder.expect(':library:mergeDebugConsumerProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeDebugGeneratedProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeReleaseConsumerProguardFiles', FROM_CACHE)
        builder.expect(':library:mergeReleaseGeneratedProguardFiles', FROM_CACHE)
        builder.expect(':app:dataBindingTriggerDebug', FROM_CACHE)
        builder.expect(':app:dataBindingTriggerRelease', FROM_CACHE)
        builder.expect(':library:dataBindingTriggerDebug', FROM_CACHE)
        builder.expect(':library:dataBindingTriggerRelease', FROM_CACHE)
        builder.expect(':app:checkDebugAarMetadata', FROM_CACHE)
        builder.expect(':app:checkReleaseAarMetadata', FROM_CACHE)
        builder.expect(':library:writeDebugAarMetadata', FROM_CACHE)
        builder.expect(':library:writeReleaseAarMetadata', FROM_CACHE)
        builder.expect(':app:writeDebugAppMetadata', FROM_CACHE)
        builder.expect(':app:writeReleaseAppMetadata', FROM_CACHE)
        builder.expect(':app:writeDebugSigningConfigVersions', FROM_CACHE)
        builder.expect(':app:writeReleaseSigningConfigVersions', FROM_CACHE)
    }

    static void android71xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:createDebugApkListingFileRedirect', SUCCESS)
        builder.expect(':app:createReleaseApkListingFileRedirect', SUCCESS)
    }

    static void android71xOnlyExpectations(ExpectedOutcomeBuilder builder) {
        // Previously were `NO_SOURCE` but now `FROM_CACHE`
        builder.expect(':library:bundleLibResDebug', FROM_CACHE)
        builder.expect(':library:bundleLibResRelease', FROM_CACHE)
    }

    static void android72xOrHigherExpectations(ExpectedOutcomeBuilder builder) {
        builder.expect(':app:checkDebugAarMetadata', SUCCESS)
        builder.expect(':app:checkReleaseAarMetadata', SUCCESS)
        builder.expect(':app:checkDebugDuplicateClasses', SUCCESS)
        builder.expect(':app:checkReleaseDuplicateClasses', SUCCESS)
        builder.expect(':app:createDebugCompatibleScreenManifests', SUCCESS)
        builder.expect(':app:createReleaseCompatibleScreenManifests', SUCCESS)
        builder.expect(':app:dataBindingTriggerDebug', SUCCESS)
        builder.expect(':app:dataBindingTriggerRelease', SUCCESS)
        builder.expect(':app:validateSigningDebug', SUCCESS)
        builder.expect(':app:writeDebugAppMetadata', SUCCESS)
        builder.expect(':app:writeReleaseAppMetadata', SUCCESS)
        builder.expect(':app:writeDebugSigningConfigVersions', SUCCESS)
        builder.expect(':app:writeReleaseSigningConfigVersions', SUCCESS)
        builder.expect(':library:bundleLibResDebug', SUCCESS)
        builder.expect(':library:bundleLibResRelease', SUCCESS)
        builder.expect(':library:dataBindingTriggerDebug', SUCCESS)
        builder.expect(':library:dataBindingTriggerRelease', SUCCESS)
        builder.expect(':library:mergeDebugConsumerProguardFiles', SUCCESS)
        builder.expect(':library:mergeDebugGeneratedProguardFiles', SUCCESS)
        builder.expect(':library:mergeReleaseConsumerProguardFiles', SUCCESS)
        builder.expect(':library:mergeReleaseGeneratedProguardFiles', SUCCESS)
        builder.expect(':library:writeDebugAarMetadata', SUCCESS)
        builder.expect(':library:writeReleaseAarMetadata', SUCCESS)
    }
}
