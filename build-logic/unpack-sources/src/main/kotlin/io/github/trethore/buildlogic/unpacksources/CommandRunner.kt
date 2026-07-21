package io.github.trethore.buildlogic.unpacksources

import org.gradle.process.ExecOperations
import java.io.File

internal class CommandRunner(
    private val execOperations: ExecOperations,
    private val rootDirectory: File,
) {
    fun run(command: List<String>) {
        execOperations.exec {
            workingDir(rootDirectory)
            commandLine(command)
        }
    }

    fun runJava(classpath: Iterable<File>, mainClassName: String, arguments: List<String>) {
        execOperations.javaexec {
            workingDir(rootDirectory)
            classpath(classpath)
            mainClass.set(mainClassName)
            args(arguments)
        }
    }
}
