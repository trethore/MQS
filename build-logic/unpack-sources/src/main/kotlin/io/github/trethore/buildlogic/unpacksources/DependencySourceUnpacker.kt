package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import java.io.File

internal class DependencySourceUnpacker(
    private val logger: Logger,
    private val rootDirectory: File,
    private val fileSystemOperations: FileSystemOperations,
    private val cfrDecompiler: CfrDecompiler,
    private val archiveUnpacker: ArchiveUnpacker,
    private val options: UnpackOptions,
    dependencyArtifactDirectories: Set<File>,
    loomMinecraftArtifacts: Set<File>,
    nestedArtifactsDirectory: File,
) {
    private val artifactResolver = ArtifactResolver(
        dependencyArtifactDirectories,
        loomMinecraftArtifacts,
    )
    private val nestedJarUnpacker = NestedJarUnpacker(
        logger,
        rootDirectory,
        archiveUnpacker,
        cfrDecompiler,
        nestedArtifactsDirectory,
    )

    fun unpack(coordinate: ModuleCoordinate, referencesDir: File) {
        unpackCoordinate(coordinate, artifactResolver.resolveBinaryArtifact(coordinate), referencesDir)
    }

    private fun unpackCoordinate(
        coordinate: ModuleCoordinate,
        binaryArtifact: File?,
        referencesDir: File,
    ) {
        val targetName = ReferencePaths.coordinatePath(coordinate)
        val targetDir = referencesDir.resolve(targetName)

        fileSystemOperations.delete {
            delete(targetDir)
        }
        targetDir.mkdirs()

        val relativeTargetDir = ReferencePaths.relativeToRoot(rootDirectory, targetDir)
        val sourceArtifact = artifactResolver.resolveSourceArtifact(coordinate)
        if (sourceArtifact != null) {
            logger.lifecycle("Unpacking sources for ${coordinate.label} -> $relativeTargetDir")
            archiveUnpacker.unpackArchive(sourceArtifact, targetDir)
            if (binaryArtifact != null) {
                archiveUnpacker.unpackBinaryResources(binaryArtifact, targetDir)
                nestedJarUnpacker.unpackIfRequested(binaryArtifact, targetDir, options)
            }
            return
        }

        require(binaryArtifact != null) {
            "Could not resolve binary artifact for ${coordinate.label}."
        }

        logger.lifecycle(
            "No source jar for ${coordinate.label}; decompiling with CFR -> $relativeTargetDir"
        )
        archiveUnpacker.unpackBinaryResources(binaryArtifact, targetDir)
        cfrDecompiler.decompile(binaryArtifact, targetDir)
        nestedJarUnpacker.unpackIfRequested(binaryArtifact, targetDir, options)
    }
}
