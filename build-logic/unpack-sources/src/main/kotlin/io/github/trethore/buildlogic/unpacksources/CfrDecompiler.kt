package io.github.trethore.buildlogic.unpacksources

import java.io.File

internal class CfrDecompiler(
    private val commandRunner: CommandRunner,
    private val classpath: List<File>,
) {
    fun decompile(artifact: File, targetDir: File) {
        commandRunner.runJava(
            classpath,
            "org.benf.cfr.reader.Main",
            listOf(
                artifact.absolutePath,
                "--outputdir",
                targetDir.absolutePath,
                "--silent",
                "true",
            ),
        )
    }

    companion object {
        fun fromClasspath(files: Iterable<File>, commandRunner: CommandRunner): CfrDecompiler {
            val classpath = files.sortedBy(File::getAbsolutePath)
            return CfrDecompiler(commandRunner, classpath)
        }
    }
}
