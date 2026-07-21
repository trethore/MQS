package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.GradleException
import java.io.File

internal class ArtifactResolver(
    private val dependencyArtifactDirectories: Set<File>,
    private val loomMinecraftArtifacts: Set<File>,
) {
    fun resolveSourceArtifact(coordinate: ModuleCoordinate): File? {
        if (isMinecraft(coordinate)) {
            return null
        }
        return resolveStagedArtifact(coordinate, "sources", required = false)
    }

    fun resolveBinaryArtifact(coordinate: ModuleCoordinate): File? {
        return resolveMinecraftArtifactFromLoom(coordinate)
            ?: resolveSingleArtifact(coordinate)
    }

    private fun resolveSingleArtifact(coordinate: ModuleCoordinate): File? {
        return resolveStagedArtifact(coordinate, "binary", required = true)
    }

    private fun resolveMinecraftArtifactFromLoom(coordinate: ModuleCoordinate): File? {
        if (!isMinecraft(coordinate)) {
            return null
        }

        val resolvedFiles = loomMinecraftArtifacts
            .flatMap { file ->
                when {
                    file.isDirectory -> file.walkTopDown().toList()
                    else -> listOf(file)
                }
            }
            .filter { file -> file.isFile && file.extension.equals("jar", ignoreCase = true) }
            .toList()

        return resolvedFiles.firstOrNull { file ->
            file.name.contains(coordinate.name) && file.name.contains(coordinate.version)
        } ?: resolvedFiles.firstOrNull { file ->
            file.name.contains(coordinate.name)
        }
    }

    private fun resolveStagedArtifact(
        coordinate: ModuleCoordinate,
        artifactType: String,
        required: Boolean,
    ): File? {
        val files = dependencyArtifactDirectories
            .map { directory ->
                directory.resolve(ReferencePaths.coordinatePath(coordinate)).resolve(artifactType)
            }
            .flatMap { directory -> directory.listFiles().orEmpty().asIterable() }
            .filter(File::isFile)
        if (files.size > 1) {
            throw GradleException(
                "Expected at most one $artifactType artifact for ${coordinate.label}, found ${files.size}."
            )
        }
        if (required && files.isEmpty()) {
            throw GradleException("Could not resolve $artifactType artifact for ${coordinate.label}.")
        }
        return files.singleOrNull()
    }

    private fun isMinecraft(coordinate: ModuleCoordinate): Boolean {
        return coordinate.group == "com.mojang" && coordinate.name == "minecraft"
    }
}
