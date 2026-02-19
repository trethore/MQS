package tytoo.myqolscripts

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Gradle task that unpacks library sources, Minecraft genSources JARs, and extracts
 * Fabric API sources into a browsable directory structure for IDE navigation.
 */
abstract class UnpackSourcesTask : DefaultTask() {

    companion object {
        private const val JAR_SUFFIX = ".jar"
        private const val SOURCES_SUFFIX = "-sources"
        private const val SOURCES_JAR_SUFFIX = "-sources.jar"
        private const val BACKUP_JAR_SUFFIX = ".backup.jar"
        private const val MINECRAFT_CLIENT_PREFIX = "minecraft-clientOnly-"
        private const val MINECRAFT_COMMON_PREFIX = "minecraft-common-"
        private const val MINECRAFT_DIR_NAME = "minecraft"
        private const val FABRIC_DIR_NAME = "fabric"
        private const val COMMON_DIR_NAME = "common"
        private const val CLIENT_DIR_NAME = "client"
        private const val MINECRAFT_SOURCES_WARNING = "Could not locate minecraft sources jars in loom cache"
        private const val FABRIC_NO_SOURCES_WARNING = "Fabric cache found but no sources jars were present"
        private const val FABRIC_CACHE_MISSING_WARNING = "Fabric cache directory not found, skipping fabric unpack"
    }

    // Input Properties

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDeps: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val minecraftCacheDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fabricCacheDir: DirectoryProperty


    // Output Properties

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // Injected Services

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    // Task Action

    @TaskAction
    fun runTask() {
        val outputDirFile = outputDir.get().asFile
        outputDirFile.mkdirs()

        unpackSourceDependencies(outputDirFile)
        unpackMinecraftSources(outputDirFile)
        unpackFabricSources(outputDirFile)
    }

    // Source Dependencies
    private fun unpackSourceDependencies(outputDirFile: File) {
        sourceDeps.files.forEach { srcJar ->
            val baseName = srcJar.name
                .removeSuffix(JAR_SUFFIX)
                .removeSuffix(SOURCES_SUFFIX)
            val targetDir = File(outputDirFile, baseName)
            unpackJarSources(srcJar, targetDir)
        }
    }

    // Minecraft Sources
    private fun unpackMinecraftSources(outputDirFile: File) {
        val minecraftClientSourcesJar = findMinecraftSourcesJar(MINECRAFT_CLIENT_PREFIX)
        val minecraftCommonSourcesJar = findMinecraftSourcesJar(MINECRAFT_COMMON_PREFIX)

        if (minecraftClientSourcesJar == null && minecraftCommonSourcesJar == null) {
            logger.warn(MINECRAFT_SOURCES_WARNING)
            return
        }

        val minecraftTarget = File(outputDirFile, MINECRAFT_DIR_NAME)
        fileSystemOperations.delete { delete(minecraftTarget) }
        minecraftTarget.mkdirs()

        minecraftCommonSourcesJar?.let {
            unpackJarSources(it, File(minecraftTarget, COMMON_DIR_NAME))
        }
        minecraftClientSourcesJar?.let {
            unpackJarSources(it, File(minecraftTarget, CLIENT_DIR_NAME))
        }
    }

    private fun unpackJarSources(jarFile: File, targetDir: File) {
        fileSystemOperations.delete { delete(targetDir) }
        targetDir.mkdirs()

        fileSystemOperations.copy {
            from(archiveOperations.zipTree(jarFile))
            into(targetDir)
        }
    }

    private fun findMinecraftSourcesJar(prefix: String): File? {
        val root = existingDirectory(minecraftCacheDir) ?: return null

        return root.walkTopDown()
            .filter { it.isFile }
            .filter { file ->
                file.name.startsWith(prefix) &&
                    file.name.endsWith(SOURCES_JAR_SUFFIX) &&
                    !file.name.endsWith(BACKUP_JAR_SUFFIX)
            }
            .maxByOrNull { it.lastModified() }
    }

    // Fabric Sources
    private fun unpackFabricSources(outputDirFile: File) {
        val fabricJars = findFabricSourceJars()

        if (fabricJars.isEmpty()) {
            val warningMessage = if (fabricCacheDir.isPresent) FABRIC_NO_SOURCES_WARNING else FABRIC_CACHE_MISSING_WARNING
            logger.warn(warningMessage)
            return
        }

        val fabricTarget = File(outputDirFile, FABRIC_DIR_NAME)
        fileSystemOperations.delete { delete(fabricTarget) }
        fabricTarget.mkdirs()

        fabricJars.forEach { jar ->
            val moduleName = jar.name.removeSuffix(SOURCES_JAR_SUFFIX)
            val moduleTarget = File(fabricTarget, moduleName)
            unpackJarSources(jar, moduleTarget)
        }
    }

    private fun findFabricSourceJars(): List<File> {
        val root = existingDirectory(fabricCacheDir) ?: return emptyList()

        val remappedModuleDirectories = root.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?: emptySequence()

        return remappedModuleDirectories
            .flatMap { directory -> directory.walkTopDown() }
            .filter { it.isFile }
            .filter { file -> file.name.endsWith(SOURCES_JAR_SUFFIX) && !file.name.endsWith(BACKUP_JAR_SUFFIX) }
            .toList()
    }

    private fun existingDirectory(directoryProperty: DirectoryProperty): File? {
        if (!directoryProperty.isPresent) {
            return null
        }

        val directory = directoryProperty.get().asFile
        if (!directory.exists()) {
            return null
        }

        return directory
    }
}
