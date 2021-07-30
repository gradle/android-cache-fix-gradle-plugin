package org.gradle.android

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class WarningsService implements BuildService<BuildServiceParameters.None>, AutoCloseable {
    @Override
    void close() throws Exception {
        Warnings.resetAll()
    }
}
