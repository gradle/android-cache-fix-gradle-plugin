package org.gradle.android.workarounds.room.operations

import groovy.transform.CompileStatic
import org.gradle.api.file.Directory
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.provider.Provider

@CompileStatic
class FileSchemaOperations {
    FileOperations fileOperations

    FileSchemaOperations(FileOperations fileOperations) {
        this.fileOperations = fileOperations
    }

    void sync(Provider<Directory> origin, Provider<Directory> destination) {
        if (origin.isPresent()) {
            fileOperations.sync {
                it.from origin
                it.into destination
            }
        }
    }
}
