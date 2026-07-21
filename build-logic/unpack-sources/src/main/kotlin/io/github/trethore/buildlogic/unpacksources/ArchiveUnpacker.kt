package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import java.io.File

internal class ArchiveUnpacker(
    private val fileSystemOperations: FileSystemOperations,
    private val archiveOperations: ArchiveOperations,
) {
    fun unpackArchive(archive: File, targetDir: File) {
        when {
            ArchiveFiles.isArchive(archive) -> fileSystemOperations.copy {
                from(archiveOperations.zipTree(archive))
                into(targetDir)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            else -> fileSystemOperations.copy {
                from(archive)
                into(targetDir)
            }
        }
    }

    fun unpackBinaryResources(archive: File, targetDir: File) {
        if (!ArchiveFiles.isArchive(archive)) {
            return
        }

        fileSystemOperations.copy {
            from(archiveOperations.zipTree(archive)) {
                exclude("**/*.class")
            }
            into(targetDir)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}
