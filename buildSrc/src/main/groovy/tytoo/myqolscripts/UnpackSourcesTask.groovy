package tytoo.myqolscripts

import groovy.io.FileType
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class UnpackSourcesTask extends DefaultTask {
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract ConfigurableFileCollection getSourceDeps()

	@InputFiles
	@Classpath
	abstract ConfigurableFileCollection getCfrClasspath()

	@OutputDirectory
	abstract DirectoryProperty getOutputDir()

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract DirectoryProperty getMinecraftCacheDir()

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	abstract DirectoryProperty getFabricCacheDir()

	@Inject
	protected abstract FileSystemOperations getFileSystemOperations()

	@Inject
	protected abstract ExecOperations getExecOperations()

	@Inject
	protected abstract ArchiveOperations getArchiveOperations()

	@TaskAction
	void runTask() {
		File outputDirFile = getOutputDir().get().asFile
		outputDirFile.mkdirs()

		getSourceDeps().files.each { File srcJar ->
			String baseName = srcJar.name
					.replaceFirst(/\.jar$/, '')
					.replaceFirst(/-sources$/, '')
			File targetDir = new File(outputDirFile, baseName)
			fileSystemOperations.delete { delete(targetDir) }
			fileSystemOperations.copy {
				from archiveOperations.zipTree(srcJar)
				into targetDir
			}
		}

		File minecraftJar = findMinecraftJar()
		if (minecraftJar != null) {
			File minecraftTarget = new File(outputDirFile, "minecraft")
			fileSystemOperations.delete { delete(minecraftTarget) }
			minecraftTarget.mkdirs()
			execOperations.javaexec {
				mainClass.set('org.benf.cfr.reader.Main')
				classpath(getCfrClasspath())
				args(minecraftJar.absolutePath, '--outputdir', minecraftTarget.absolutePath)
			}
		} else {
			logger.warn("Could not locate minecraft client jar in loom cache")
		}

		List<File> fabricJars = findFabricSourceJars()
		if (!fabricJars.isEmpty()) {
			File fabricTarget = new File(outputDirFile, "fabric")
			fileSystemOperations.delete { delete(fabricTarget) }
			fabricTarget.mkdirs()
			fabricJars.each { File jar ->
				String moduleName = jar.name.replaceFirst(/-sources\.jar$/, '')
				File moduleTarget = new File(fabricTarget, moduleName)
				fileSystemOperations.delete { delete(moduleTarget) }
				fileSystemOperations.copy {
					from archiveOperations.zipTree(jar)
					into moduleTarget
				}
			}
		} else if (getFabricCacheDir().isPresent()) {
			logger.warn("Fabric cache found but no sources jars were present")
		} else {
			logger.warn("Fabric cache directory not found, skipping fabric unpack")
		}
	}

	private File findMinecraftJar() {
		if (!getMinecraftCacheDir().isPresent()) {
			return null
		}
		File root = getMinecraftCacheDir().get().asFile
		if (!root.exists()) {
			return null
		}
		File latest = null
		root.eachFileRecurse(FileType.FILES) { File file ->
			if (file.name.startsWith("minecraft-clientOnly-") && file.name.endsWith(".jar") && !file.name.endsWith(".backup.jar")) {
				if (latest == null || file.lastModified() > latest.lastModified()) {
					latest = file
				}
			}
		}
		return latest
	}

	private List<File> findFabricSourceJars() {
		if (!getFabricCacheDir().isPresent()) {
			return new ArrayList<>()
		}
		File root = getFabricCacheDir().get().asFile
		if (!root.exists()) {
			return new ArrayList<>()
		}
		List<File> jars = new ArrayList<>()
		root.eachFileRecurse(FileType.FILES) { File file ->
			if (file.name.endsWith("-sources.jar")) {
				jars.add(file)
			}
		}
		return jars
	}
}
