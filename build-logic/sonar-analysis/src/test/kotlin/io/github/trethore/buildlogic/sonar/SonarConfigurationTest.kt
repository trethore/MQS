package io.github.trethore.buildlogic.sonar

import org.gradle.api.GradleException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SonarConfigurationTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `loads properties and issue exclusions`() {
        val file = temporaryDirectory.resolve("analysis.json")
        file.writeText(
            """
            {
              "properties": {"sonar.inclusions": ["**/*.java"], "sonar.gradle.scanAll": false},
              "issueExclusions": [{"ruleKey": "java:S4032", "filePattern": "**/package-info.java"}]
            }
            """.trimIndent()
        )
        val configuration = SonarConfiguration.load(file.toFile())
        assertEquals(listOf("**/*.java"), configuration.properties["sonar.inclusions"])
        assertEquals(false, configuration.properties["sonar.gradle.scanAll"])
        assertEquals(
            listOf(SonarIssueExclusion("java:S4032", "**/package-info.java")),
            configuration.issueExclusions,
        )
    }

    @Test
    fun `rejects unknown keys and direct multicriteria properties`() {
        val unknown = temporaryDirectory.resolve("unknown.json")
        unknown.writeText("""{"unknown": true}""")
        assertFailsWith<GradleException> { SonarConfiguration.load(unknown.toFile()) }

        val multicriteria = temporaryDirectory.resolve("multicriteria.json")
        multicriteria.writeText(
            """{"properties":{"sonar.issue.ignore.multicriteria":"rule"}}"""
        )
        assertFailsWith<GradleException> { SonarConfiguration.load(multicriteria.toFile()) }
    }
}
