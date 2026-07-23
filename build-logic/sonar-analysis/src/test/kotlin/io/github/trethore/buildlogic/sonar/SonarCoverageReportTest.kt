package io.github.trethore.buildlogic.sonar

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SonarCoverageReportTest {
    @Test
    fun `loads and renders files with uncovered lines`() {
        val client = FakeSonarClient { request ->
            when (request.path) {
                "/api/measures/component" -> mapOf(
                    "component" to mapOf(
                        "measures" to measures(
                            "coverage" to "80.0",
                            "line_coverage" to "85.0",
                            "branch_coverage" to "70.0",
                            "new_coverage" to "90.0",
                            "lines_to_cover" to "20",
                            "uncovered_lines" to "3",
                        )
                    )
                )
                "/api/measures/component_tree" -> mapOf(
                    "components" to listOf(
                        mapOf(
                            "path" to "src/Foo.java",
                            "measures" to measures(
                                "line_coverage" to "92.9",
                                "lines_to_cover" to "14",
                                "uncovered_lines" to "1",
                            ),
                        ),
                        mapOf(
                            "path" to "src/Complete.java",
                            "measures" to measures(
                                "line_coverage" to "100.0",
                                "lines_to_cover" to "8",
                                "uncovered_lines" to "0",
                            ),
                        ),
                    ),
                    "paging" to mapOf("total" to 2),
                )
                else -> error("Unexpected request: $request")
            }
        }
        val report = SonarCoverageLoader(client).load("mqp")
        assertEquals(listOf(SonarCoverageFile("src/Foo.java", "92.9", 13, 14)), report.files)
        assertEquals(
            listOf(
                "SonarQube coverage for mqp:",
                "  Overall: 80.0%",
                "  Lines: 85.0% (17/20 lines)",
                "  Branches: 70.0%",
                "  New code: 90.0%",
                "",
                "Files with uncovered lines:",
                "  src/Foo.java: 92.9% (13/14 covered)",
            ),
            SonarCoverageRenderer.render(report),
        )
    }

    @Test
    fun `does not load files when all lines are covered`() {
        val client = FakeSonarClient {
            mapOf(
                "component" to mapOf(
                    "measures" to measures(
                        "line_coverage" to "100.0",
                        "lines_to_cover" to "10",
                        "uncovered_lines" to "0",
                    )
                )
            )
        }
        val report = SonarCoverageLoader(client).load("mqp")
        assertEquals(1, client.requests.size)
        assertFalse(SonarCoverageRenderer.render(report).contains("Files with uncovered lines:"))
    }
}
