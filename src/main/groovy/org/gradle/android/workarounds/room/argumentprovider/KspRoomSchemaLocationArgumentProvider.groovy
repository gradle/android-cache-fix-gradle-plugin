package org.gradle.android.workarounds.room.argumentprovider

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal

class KspRoomSchemaLocationArgumentProvider extends RoomSchemaLocationArgumentProvider {
    @Internal Provider<Directory> temporarySchemaLocationDir

    KspRoomSchemaLocationArgumentProvider(Project project, Provider<Directory> configuredSchemaLocationDir, Provider<Directory> schemaLocationDir) {
        super(project, configuredSchemaLocationDir, schemaLocationDir)
        this.temporarySchemaLocationDir = schemaLocationDir.map { it.dir("../${it.asFile.name}Temp") }
    }

    @Override
    protected String getSchemaLocationPath() {
        return projectDir.relativePath(temporarySchemaLocationDir.get().asFile)
    }
}
