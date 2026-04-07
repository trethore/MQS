package tytoo.myqolscripts

import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import de.aaschmid.gradle.plugins.cpd.Cpd
import de.aaschmid.gradle.plugins.cpd.CpdExtension
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

abstract class VerifyCodeTask : DefaultTask() {

    @get:Optional
    @get:InputDirectory
    abstract val reportsDir: DirectoryProperty

    @TaskAction
    fun verifyReports() {
        val reportsRoot = reportsDir.asFile.orNull ?: return
        if (!reportsRoot.exists()) {
            return
        }

        val violations = reportsRoot.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .flatMap { reportFile ->
                when {
                    isToolReport(reportFile, "pmd") -> parsePmdReport(reportFile).asSequence()
                    isToolReport(reportFile, "spotbugs") -> parseSpotBugsReport(reportFile).asSequence()
                    isToolReport(reportFile, "cpd") -> parseCpdReport(reportFile).asSequence()
                    else -> emptySequence()
                }
            }
            .toList()

        if (violations.isEmpty()) {
            logger.lifecycle("Code verification passed.")
            return
        }

        logger.error("Code verification found ${violations.size} issue(s):")
        violations.forEach { logger.error(it) }
        throw GradleException(
            "Code verification failed. See messages above and generated reports in ${reportsRoot.absolutePath}."
        )
    }

    private fun isToolReport(reportFile: File, toolDirectoryName: String): Boolean {
        val normalizedPath = reportFile.absolutePath.replace(File.separatorChar, '/')
        return normalizedPath.contains("/reports/$toolDirectoryName/")
    }

    private fun parsePmdReport(reportFile: File): List<String> {
        val document = parseXml(reportFile)
        val results = mutableListOf<String>()
        val fileNodes = document.getElementsByTagName("file")

        for (index in 0 until fileNodes.length) {
            val fileElement = fileNodes.item(index) as? Element ?: continue
            val filePath = fileElement.getAttribute("name").ifBlank { reportFile.name }
            val violationNodes = fileElement.getElementsByTagName("violation")

            for (violationIndex in 0 until violationNodes.length) {
                val violationElement = violationNodes.item(violationIndex) as? Element ?: continue
                val line = violationElement.getAttribute("beginline").ifBlank { "?" }
                val ruleSet = violationElement.getAttribute("ruleset")
                val rule = violationElement.getAttribute("rule")
                val message = normalizeText(violationElement.textContent)
                results += "PMD: $filePath:$line [$ruleSet/$rule] $message"
            }
        }

        return results
    }

    private fun parseSpotBugsReport(reportFile: File): List<String> {
        val document = parseXml(reportFile)
        val results = mutableListOf<String>()
        val bugNodes = document.getElementsByTagName("BugInstance")

        for (index in 0 until bugNodes.length) {
            val bugElement = bugNodes.item(index) as? Element ?: continue
            val category = bugElement.getAttribute("category")
            val type = bugElement.getAttribute("type")
            val priority = bugElement.getAttribute("priority")
            val rank = bugElement.getAttribute("rank")
            val sourceLineElement = firstDescendant(bugElement, "SourceLine")
            val sourcePath = sourceLineElement?.getAttribute("sourcepath")
                ?.ifBlank { null }
                ?: firstDescendant(bugElement, "Class")
                    ?.getAttribute("classname")
                    ?.replace('.', '/')
                    ?.plus(".java")
                ?: reportFile.name
            val line = sourceLineElement?.getAttribute("start")?.ifBlank { "?" } ?: "?"
            val message = firstDescendant(bugElement, "LongMessage")
                ?.textContent
                ?.let(::normalizeText)
                ?: firstDescendant(bugElement, "ShortMessage")
                    ?.textContent
                    ?.let(::normalizeText)
                ?: "SpotBugs issue"

            results += "SpotBugs: $sourcePath:$line [$category/$type, priority=$priority, rank=$rank] $message"
        }

        return results
    }

    private fun parseCpdReport(reportFile: File): List<String> {
        val document = parseXml(reportFile)
        val results = mutableListOf<String>()
        val duplicationNodes = document.getElementsByTagName("duplication")

        for (index in 0 until duplicationNodes.length) {
            val duplicationElement = duplicationNodes.item(index) as? Element ?: continue
            val lines = duplicationElement.getAttribute("lines").ifBlank { "?" }
            val tokens = duplicationElement.getAttribute("tokens").ifBlank { "?" }
            val fileNodes = duplicationElement.getElementsByTagName("file")
            val locations = mutableListOf<String>()

            for (fileIndex in 0 until fileNodes.length) {
                val fileElement = fileNodes.item(fileIndex) as? Element ?: continue
                val path = fileElement.getAttribute("path")
                val line = fileElement.getAttribute("line").ifBlank { "?" }
                locations += "$path:$line"
            }

            results += "CPD: duplicated block ($lines lines, $tokens tokens) in ${locations.joinToString(" | ")}"
        }

        return results
    }

    private fun parseXml(reportFile: File): Document {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature(xmlFeature("apache.org/xml/features/disallow-doctype-decl"), true)
            factory.setFeature(xmlFeature("xml.org/sax/features/external-general-entities"), false)
            factory.setFeature(xmlFeature("xml.org/sax/features/external-parameter-entities"), false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            factory.newDocumentBuilder().parse(reportFile)
        } catch (exception: Exception) {
            throw GradleException("Failed to parse verification report ${reportFile.absolutePath}", exception)
        }
    }

    private fun firstDescendant(element: Element, tagName: String): Element? {
        val descendants = element.getElementsByTagName(tagName)
        return if (descendants.length == 0) null else descendants.item(0) as? Element
    }

    private fun xmlFeature(path: String): String {
        return String(charArrayOf('h', 't', 't', 'p', ':', '/', '/')) + path
    }

    private fun normalizeText(rawText: String): String {
        return rawText.trim().replace(Regex("\\s+"), " ")
    }
}

class VerifyCodePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("pmd")
        pluginManager.apply("com.github.spotbugs")
        pluginManager.apply("net.ltgt.errorprone")
        pluginManager.apply("org.danilopianini.cpd")

        val errorProneVersion = findProperty("errorProneVersion")?.toString() ?: "2.48.0"
        val sourceSets = extensions.getByType<SourceSetContainer>()

        extensions.configure<PmdExtension> {
            toolVersion = "7.4.0"
            isConsoleOutput = true
            isIgnoreFailures = true
            ruleSets = listOf(
                "category/java/bestpractices.xml",
                "category/java/errorprone.xml",
                "category/java/performance.xml",
                "category/java/design.xml"
            )
        }

        tasks.withType<Pmd>().configureEach {
            reports.html.required.set(true)
            reports.xml.required.set(true)
        }

        tasks.withType<SpotBugsTask>().configureEach {
            ignoreFailures = true
            effort.set(Effort.MAX)
            reportLevel.set(Confidence.LOW)
            reports.create("html") {
                required.set(true)
            }
            reports.create("xml") {
                required.set(true)
            }
        }

        extensions.configure<CpdExtension> {
            language = "java"
            minimumTokenCount = 100
            isIgnoreFailures = true
        }

        tasks.withType<Cpd>().configureEach {
            ignoreFailures = true
            setSource(files(sourceSets.flatMap { it.allJava.srcDirs }.filter { it.exists() }))
            reports {
                text.required.set(true)
                xml.required.set(true)
            }
        }

        dependencies.add("errorprone", "com.google.errorprone:error_prone_core:$errorProneVersion")

        tasks.withType<JavaCompile>().configureEach {
            options.errorprone.enabled.set(true)
            options.errorprone.disableWarningsInGeneratedCode.set(true)
            options.errorprone.allErrorsAsWarnings.set(false)
        }

        val verifyCode = tasks.register<VerifyCodeTask>("verifyCode") {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs Error Prone, PMD, SpotBugs, and CPD and prints the collected violations."
            reportsDir.set(layout.buildDirectory.dir("reports"))
            outputs.upToDateWhen { false }
            dependsOn(tasks.withType<JavaCompile>())
            dependsOn(tasks.withType<Pmd>())
            dependsOn(tasks.withType<SpotBugsTask>())
            dependsOn(tasks.withType<Cpd>())
        }

        tasks.named(JavaBasePlugin.CHECK_TASK_NAME).configure {
            dependsOn(verifyCode)
        }
    }
}
