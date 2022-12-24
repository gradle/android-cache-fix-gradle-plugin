package org.gradle.android.workarounds.room.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import javax.inject.Inject

/**
 * This task is intentionally not incremental.  The intention here is to duplicate the behavior the user
 * experiences when the workaround is not applied, which is to only write whatever schemas that were generated
 * during this execution, even if they are incomplete (they really shouldn't be, though).
 *
 * We don't want to create task dependencies on the compile/kapt tasks because we don't want to force execution
 * of those tasks if only a single variant is being assembled.
 */
@DisableCachingByDefault(because = 'This is a disk bound copy/merge task.')
abstract class RoomSchemaLocationMergeTask extends DefaultTask {

    // Using older internal API to maintain compatibility with Gradle 5.x
    @Inject abstract FileOperations getFileOperations()

    @Internal
    MergeAssociations roomSchemaMergeLocations

    @TaskAction
    void mergeSourcesToDestinations() {
        roomSchemaMergeLocations.mergeAssociations.each { destination, source ->
            logger.info("Merging schemas to ${destination.get().asFile}")
            fileOperations.copy {
                it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                it.into(destination)
                it.from(source)
            }
        }
    }
}

