package org.gradle.android.workarounds.room.argumentprovider

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

@CompileStatic
class JavaCompilerRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
    JavaCompilerRoomSchemaLocationArgumentProvider(Project project, Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        super(project, configuredSchemaLocationDir, schemaLocationDir)
    }
}
