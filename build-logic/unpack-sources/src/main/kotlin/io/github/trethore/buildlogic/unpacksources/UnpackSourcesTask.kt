package io.github.trethore.buildlogic.unpacksources

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class UnpackSourcesTask : DefaultTask() {
    @get:Input
    abstract val dependencyCoordinates: ListProperty<String>

    @get:Input
    abstract val gitReferences: ListProperty<String>

    @get:Input
    abstract val unpackNestedJars: Property<Boolean>

    @get:Classpath
    abstract val cfrClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val loomMinecraftArtifacts: ConfigurableFileCollection

    @get:Classpath
    abstract val dependencyArtifacts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun unpack() {
        val referencesDir = outputDirectory.get().asFile
        val rootDir = rootDirectory.get().asFile
        fileSystemOperations.delete {
            delete(referencesDir)
        }
        referencesDir.mkdirs()

        val cfrDecompiler = CfrDecompiler.fromClasspath(
            cfrClasspath.files,
            CommandRunner(execOperations, rootDir),
        )
        val options = UnpackOptions(unpackNestedJars.get())
        val archiveUnpacker = ArchiveUnpacker(fileSystemOperations, archiveOperations)
        val dependencySourceUnpacker = DependencySourceUnpacker(
            logger,
            rootDir,
            fileSystemOperations,
            cfrDecompiler,
            archiveUnpacker,
            options,
            dependencyArtifacts.files,
            loomMinecraftArtifacts.files,
            temporaryDir.resolve("nested"),
        )

        dependencyCoordinates.get().sorted().forEach { coordinateLabel ->
            dependencySourceUnpacker.unpack(ModuleCoordinate.parse(coordinateLabel), referencesDir)
        }

        GitReferenceUnpacker(
            logger,
            rootDir,
            fileSystemOperations,
            CommandRunner(execOperations, rootDir),
        ).unpackAll(
            gitReferences.get().map(GitReference::deserialize),
            referencesDir,
        )
    }
}
