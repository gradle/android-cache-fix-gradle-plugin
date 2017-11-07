package org.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class AbstractTest extends Specification {
    @Rule TemporaryFolder temporaryFolder
    File cacheDir

    def setup() {
        cacheDir = temporaryFolder.newFolder()
    }

    def withGradleVersion(String gradleVersion) {
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withDebug(false)
    }
}
