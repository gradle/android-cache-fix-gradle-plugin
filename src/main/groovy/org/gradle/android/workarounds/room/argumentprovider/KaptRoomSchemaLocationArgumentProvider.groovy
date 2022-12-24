package org.gradle.android.workarounds.room.argumentprovider

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

class KaptRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
    private Provider<Directory> temporarySchemaLocationDir

    KaptRoomSchemaLocationArgumentProvider(Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        super(configuredSchemaLocationDir, schemaLocationDir)
        this.temporarySchemaLocationDir = schemaLocationDir.map {it.dir("../${it.asFile.name}Temp") }
    }

    @Override
    protected String getSchemaLocationPath() {
        return temporarySchemaLocationDir.get().asFile.absolutePath
    }
}
