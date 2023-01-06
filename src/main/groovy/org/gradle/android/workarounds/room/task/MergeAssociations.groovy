package org.gradle.android.workarounds.room.task

import groovy.transform.CompileStatic
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

import javax.inject.Inject

@CompileStatic
class MergeAssociations {
    final ObjectFactory objectFactory
    final Map<Provider<Directory>, ConfigurableFileCollection> mergeAssociations = [:]

    @Inject
    MergeAssociations(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
    }

    void registerMerge(Provider<Directory> destination, Provider<Directory> source) {
        if (!mergeAssociations.containsKey(destination)) {
            mergeAssociations.put(destination, objectFactory.fileCollection())
        }

        mergeAssociations.get(destination).from(source)
    }
}
