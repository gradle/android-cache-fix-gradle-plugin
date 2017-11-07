package org.gradle.android

import spock.lang.Unroll

import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE

class RelocationTest extends AbstractTest {

    @Unroll
    def "simple Android app is relocatable with #gradleVersion and Android plugin #androidVersion"() {
        def originalDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(originalDir, cacheDir, androidVersion.toString()).writeProject()

        def relocatedDir = temporaryFolder.newFolder()
        new SimpleAndroidApp(relocatedDir, cacheDir, androidVersion.toString()).writeProject()

        def gradleVerString = gradleVersion.version

        when:
        def result = withGradleVersion(gradleVerString)
            .withProjectDir(originalDir)
            .withArguments("assemble", "--build-cache")
            .build()

        then:
        !result.output.contains("not applying workarounds")

        when:
        result = withGradleVersion(gradleVerString)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "--build-cache")
            .build()

        then:
        verifyAll {
            result.task(":app:checkDebugManifest").outcome == FROM_CACHE
            result.task(":app:checkDebugManifest").outcome == FROM_CACHE
            result.task(":app:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":app:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":app:compileDebugAidl").outcome == FROM_CACHE
            result.task(":app:compileDebugJavaWithJavac").outcome == FROM_CACHE
            result.task(":app:compileDebugRenderscript").outcome == FROM_CACHE
            result.task(":app:compileDebugShaders").outcome == FROM_CACHE
            result.task(":app:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":app:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":app:compileReleaseJavaWithJavac").outcome == FROM_CACHE
            result.task(":app:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":app:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":app:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":app:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":app:createDebugCompatibleScreenManifests").outcome == FROM_CACHE
            result.task(":app:createReleaseCompatibleScreenManifests").outcome == FROM_CACHE
            result.task(":app:generateDebugBuildConfig").outcome == FROM_CACHE
            result.task(":app:generateDebugResValues").outcome == FROM_CACHE
            result.task(":app:generateReleaseBuildConfig").outcome == FROM_CACHE
            result.task(":app:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":app:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":app:javaPreCompileDebug").outcome == FROM_CACHE
            result.task(":app:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":app:mergeDebugAssets").outcome == FROM_CACHE
            result.task(":app:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeDebugResources").outcome == FROM_CACHE
            result.task(":app:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":app:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":app:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseResources").outcome == FROM_CACHE
            result.task(":app:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":app:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":app:preDebugBuild").outcome == FROM_CACHE
            result.task(":app:preReleaseBuild").outcome == FROM_CACHE
            result.task(":app:preReleaseBuild").outcome == FROM_CACHE
            result.task(":app:processDebugManifest").outcome == FROM_CACHE
            result.task(":app:processDebugResources").outcome == FROM_CACHE
            result.task(":app:processReleaseManifest").outcome == FROM_CACHE
            result.task(":app:processReleaseResources").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskDebug").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskRelease").outcome == FROM_CACHE
            result.task(":app:splitsDiscoveryTaskRelease").outcome == FROM_CACHE
            // TODO This loads from cache on macOS, but on Linux it somehow gets executed
            // result.task(":app:transformDexArchiveWithDexMergerForDebug").outcome == FROM_CACHE
            result.task(":library:checkDebugManifest").outcome == FROM_CACHE
            result.task(":library:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":library:checkReleaseManifest").outcome == FROM_CACHE
            result.task(":library:compileDebugAidl").outcome == FROM_CACHE
            result.task(":library:compileDebugJavaWithJavac").outcome == FROM_CACHE
            result.task(":library:compileDebugRenderscript").outcome == FROM_CACHE
            result.task(":library:compileDebugShaders").outcome == FROM_CACHE
            result.task(":library:compileDebugShaders").outcome == FROM_CACHE
            result.task(":library:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":library:compileReleaseAidl").outcome == FROM_CACHE
            result.task(":library:compileReleaseJavaWithJavac").outcome == FROM_CACHE
            result.task(":library:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":library:compileReleaseRenderscript").outcome == FROM_CACHE
            result.task(":library:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":library:compileReleaseShaders").outcome == FROM_CACHE
            result.task(":library:extractDebugAnnotations").outcome == FROM_CACHE
            // TODO This produces overlapping outputs in build/intermediates/typedefs.txt
            // result.task(":library:extractReleaseAnnotations").outcome == FROM_CACHE
            result.task(":library:generateDebugBuildConfig").outcome == FROM_CACHE
            result.task(":library:generateDebugResValues").outcome == FROM_CACHE
            result.task(":library:generateDebugResValues").outcome == FROM_CACHE
            result.task(":library:generateReleaseBuildConfig").outcome == FROM_CACHE
            result.task(":library:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":library:generateReleaseResValues").outcome == FROM_CACHE
            result.task(":library:javaPreCompileDebug").outcome == FROM_CACHE
            result.task(":library:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":library:javaPreCompileRelease").outcome == FROM_CACHE
            result.task(":library:mergeDebugAssets").outcome == FROM_CACHE
            result.task(":library:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeDebugJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":library:mergeDebugShaders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":library:mergeReleaseAssets").outcome == FROM_CACHE
            result.task(":library:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseJniLibFolders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":library:mergeReleaseShaders").outcome == FROM_CACHE
            result.task(":library:packageDebugResources").outcome == FROM_CACHE
            result.task(":library:packageReleaseResources").outcome == FROM_CACHE
            result.task(":library:platformAttrExtractor").outcome == FROM_CACHE
            result.task(":library:processDebugManifest").outcome == FROM_CACHE
            result.task(":library:processDebugResources").outcome == FROM_CACHE
            result.task(":library:processReleaseManifest").outcome == FROM_CACHE
            result.task(":library:processReleaseManifest").outcome == FROM_CACHE
            result.task(":library:processReleaseResources").outcome == FROM_CACHE
        }

        where:
        [gradleVersion, androidVersion] << GroovyCollections.combinations(Versions.SUPPORTED_GRADLE_VERSIONS, Versions.SUPPORTED_ANDROID_VERSIONS)
    }
}
