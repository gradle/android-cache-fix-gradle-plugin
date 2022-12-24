package org.gradle.android.workarounds.room.argumentprovider

import org.gradle.android.workarounds.RoomSchemaLocationWorkaround
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.CommandLineArgumentProvider

abstract class RoomSchemaLocationArgumentProvider implements CommandLineArgumentProvider {
    @Internal
    final Provider<Directory> configuredSchemaLocationDir

    @Internal
    final Provider<Directory> schemaLocationDir

    RoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        this.configuredSchemaLocationDir = configuredSchemaLocationDir
        this.schemaLocationDir = schemaLocationDir
    }

    @Internal
    protected String getSchemaLocationPath() {
        return schemaLocationDir.get().asFile.absolutePath
    }

    @Override
    Iterable<String> asArguments() {
        if (configuredSchemaLocationDir.isPresent()) {
            return ["-A${RoomSchemaLocationWorkaround.ROOM_SCHEMA_LOCATION}=${schemaLocationPath}" as String]
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

