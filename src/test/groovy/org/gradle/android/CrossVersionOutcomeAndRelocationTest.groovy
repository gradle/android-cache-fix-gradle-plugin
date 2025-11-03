package org.gradle.android

import com.google.common.collect.ImmutableMap
import groovy.json.JsonSlurper
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import spock.lang.Unroll

import static org.gradle.android.TestVersions.latestKotlinVersionForGradleVersion
import static org.gradle.testkit.runner.TaskOutcome.FROM_CACHE

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
            .withArguments("assemble", "createFullJarDebug", "createFullJarRelease", "--build-cache", "--stacktrace", "--info")
            .build()

        when:
        def result = withGradleVersion(gradleVersion.version)
            .withProjectDir(relocatedDir)
            .withArguments("assemble", "createFullJarDebug", "createFullJarRelease", "--build-cache", "--stacktrace","--info")
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

        ExpectedOutcomeBuilder expect(String key, String outcome) {
            checkIfSeen(key)
            mapBuilder.put(key, ExpectedOutcome.valueOf(outcome))
            return this
        }

        ImmutableMap<String, ExpectedOutcome> build() {
            return mapBuilder.build()
        }
    }

    private static ExpectedResults expectedResults(VersionNumber androidVersion, VersionNumber kotlinVersion) {
        def builder = new ExpectedOutcomeBuilder()
        def path = "expectedOutcomes/${androidVersion.major}.${androidVersion.minor}_outcomes.json"
        def outcomesResource = CrossVersionOutcomeAndRelocationTest.classLoader.getResource(path)

        if (outcomesResource == null) {
            throw new IllegalStateException("Could not find expectedOutcomes/${androidVersion}_outcomes.json - make sure an outcomes file exists for this version!")
        }

        Map<String, String> json = new JsonSlurper().parse(outcomesResource) as Map

        json.each { task, outcome ->
            builder.expect(task as String, outcome as String)
        }

        if (kotlinVersion >= VersionNumber.parse("1.6.0") && androidVersion.major < 9) {
            builder.expect(":app:buildKotlinToolingMetadata", "SUCCESS")
        }

        new ExpectedResults(
            builder.build()
        )
    }
}
