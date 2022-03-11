package org.gradle.android

import groovy.transform.CompileStatic
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

@CompileStatic
abstract class WarningsService implements BuildService<BuildServiceParameters.None>, AutoCloseable {
    @Override
    void close() throws Exception {
        Warnings.resetAll()
    }
}
