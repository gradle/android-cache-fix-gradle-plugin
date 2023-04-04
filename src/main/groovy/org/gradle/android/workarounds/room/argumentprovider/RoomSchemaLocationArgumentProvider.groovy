package org.gradle.android.workarounds.room.argumentprovider

import groovy.transform.CompileStatic
import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.CommandLineArgumentProvider

@CompileStatic
abstract class RoomSchemaLocationArgumentProvider implements CommandLineArgumentProvider {

    @Internal
    final File projectDir;

    @Internal
    final Provider<Directory> configuredSchemaLocationDir

    @Internal
    final Provider<Directory> schemaLocationDir

    RoomSchemaLocationArgumentProvider(Project project, Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        this.projectDir = project.projectDir
        this.configuredSchemaLocationDir = configuredSchemaLocationDir
        this.schemaLocationDir = schemaLocationDir
    }

    @Internal
    protected String getSchemaLocationPath() {
        return projectDir.relativePath(schemaLocationDir.get().asFile)
    }

    @Override
    Iterable<String> asArguments() {
        if (configuredSchemaLocationDir.isPresent()) {
            if (this instanceof KspRoomSchemaLocationArgumentProvider) {
                return ["${RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION}=${schemaLocationPath}" as String]
            } else {
                return ["-A${RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION}=${schemaLocationPath}" as String]
            }
        } else {
            return []
        }
    }

    @OutputDirectory
    @Optional
    Provider<Directory> getEffectiveSchemaLocationDir() {
        return schemaLocationDir
    }
}

