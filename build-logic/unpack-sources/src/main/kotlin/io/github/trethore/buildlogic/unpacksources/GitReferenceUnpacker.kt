package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URI

internal class GitReferenceUnpacker(
    private val logger: Logger,
    private val rootDirectory: File,
    private val fileSystemOperations: FileSystemOperations,
    private val commandRunner: CommandRunner,
) {
    fun unpackAll(gitReferences: Iterable<GitReference>, referencesDir: File) {
        gitReferences.forEach { gitReference ->
            unpack(gitReference, referencesDir)
        }
    }

    private fun unpack(gitReference: GitReference, referencesDir: File) {
        val label = parseGitLabel(gitReference)
        val revision = gitReference.commit ?: gitReference.branch
        val targetDir = referencesDir.resolve(ReferencePaths.safePathSegment("$label-$revision"))

        fileSystemOperations.delete {
            delete(targetDir)
        }
        targetDir.parentFile.mkdirs()

        val cloneArgs = mutableListOf("clone", "--branch", gitReference.branch)
        if (gitReference.commit == null) {
            cloneArgs += listOf("--depth", "1")
        }
        cloneArgs += listOf(gitReference.url, targetDir.absolutePath)

        val relativeTargetDir = ReferencePaths.relativeToRoot(rootDirectory, targetDir)
        logger.lifecycle("Cloning ${gitReference.url} (${gitReference.branch}) -> $relativeTargetDir")
        commandRunner.run(listOf("git") + cloneArgs)

        if (gitReference.commit != null) {
            commandRunner.run(listOf("git", "-C", targetDir.absolutePath, "checkout", gitReference.commit))
        }
    }

    private fun parseGitLabel(gitReference: GitReference): String {
        val path = gitPath(gitReference.url)
        val segments = path
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() }

        if (segments.size >= 2) {
            val owner = segments[segments.size - 2]
            val repo = segments.last().removeSuffix(".git")
            return "$owner-$repo"
        }

        return gitReference.name?.takeIf { it.isNotBlank() }
            ?: segments.lastOrNull()?.removeSuffix(".git")?.takeIf { it.isNotBlank() }
            ?: "git-reference"
    }

    private fun gitPath(url: String): String {
        return try {
            URI(url).path.takeIf { !it.isNullOrBlank() } ?: url.substringAfter(':', url)
        } catch (_: Exception) {
            url.substringAfter(':', url)
        }
    }
}
