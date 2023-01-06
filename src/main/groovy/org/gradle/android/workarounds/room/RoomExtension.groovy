package org.gradle.android.workarounds.room

import groovy.transform.CompileStatic
import org.gradle.android.workarounds.room.task.MergeAssociations
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

import javax.inject.Inject

@CompileStatic
abstract class RoomExtension {
    DirectoryProperty schemaLocationDir
    MergeAssociations roomSchemaMergeLocations

    @Inject
    RoomExtension(ObjectFactory objectFactory) {
        schemaLocationDir = objectFactory.directoryProperty()
        roomSchemaMergeLocations = objectFactory.newInstance(MergeAssociations)
    }

    void registerOutputDirectory(Provider<Directory> outputDir) {
        roomSchemaMergeLocations.registerMerge(schemaLocationDir, outputDir)
    }
}
