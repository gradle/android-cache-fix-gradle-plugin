package org.gradle.android.workarounds.room.operations

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

@CompileStatic
class FileSchemaOperations {
    Project project

    FileSchemaOperations(Project project) {
        this.project = project
    }

    void sync(Provider<Directory> origin, Provider<Directory> destination) {
        if (origin.isPresent()) {
            project.sync {
                it.from origin
                it.into destination
            }
        }
    }
}
