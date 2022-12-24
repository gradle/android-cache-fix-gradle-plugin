package org.gradle.android.workarounds.room.argumentprovider

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

class JavaCompilerRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
    JavaCompilerRoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        super(configuredSchemaLocationDir, schemaLocationDir)
    }
}
