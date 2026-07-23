package io.github.trethore.buildlogic.sonar

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SonarDuplicatesReportTest {
    @Test
    fun `skips details when no files are affected`() {
        val client = FakeSonarClient {
            mapOf(
                "component" to mapOf(
                    "measures" to measures(
                        "duplicated_files" to "0",
                        "duplicated_lines" to "0",
                        "duplicated_lines_density" to "0.0",
                    )
                )
            )
        }
        assertEquals(
            listOf(
                "Duplication summary:",
                "  Duplicate groups: 0",
                "  Affected files: 0",
                "  Duplicated lines: 0",
                "  Duplication density: 0.0%",
            ),
            SonarDuplicatesRenderer.render(SonarDuplicatesLoader(client).load("mqp")),
        )
        assertEquals(1, client.requests.size)
    }

    @Test
    fun `deduplicates groups returned for multiple files`() {
        val client = FakeSonarClient { request ->
            when (request.path) {
                "/api/measures/component" -> mapOf(
                    "component" to mapOf(
                        "measures" to measures(
                            "duplicated_files" to "2",
                            "duplicated_lines" to "6",
                            "duplicated_lines_density" to "4.2",
                        )
                    )
                )
                "/api/measures/component_tree" -> mapOf(
                    "components" to listOf(
                        mapOf(
                            "key" to "mqp:A",
                            "path" to "src/A.java",
                            "measures" to measures("duplicated_lines" to "3"),
                        ),
                        mapOf(
                            "key" to "mqp:B",
                            "path" to "src/B.java",
                            "measures" to measures("duplicated_lines" to "3"),
                        ),
                    ),
                    "paging" to mapOf("total" to 2),
                )
                "/api/duplications/show" -> mapOf(
                    "files" to mapOf(
                        "1" to mapOf("key" to "mqp:A", "name" to "A.java"),
                        "2" to mapOf("key" to "mqp:B", "name" to "B.java"),
                    ),
                    "duplications" to listOf(
                        mapOf(
                            "blocks" to listOf(
                                mapOf("_ref" to "1", "from" to 10, "size" to 3),
                                mapOf("_ref" to "2", "from" to 20, "size" to 3),
                            )
                        )
                    ),
                )
                else -> error("Unexpected request: $request")
            }
        }
        val report = SonarDuplicatesLoader(client).load("mqp")
        assertEquals(1, report.groups.size)
        assertEquals(2, client.requests.count { it.path == "/api/duplications/show" })
        assertEquals(
            listOf(
                "Duplication summary:",
                "  Duplicate groups: 1",
                "  Affected files: 2",
                "  Duplicated lines: 6",
                "  Duplication density: 4.2%",
                "",
                "Duplicate groups:",
                "  1. 3 lines, 2 occurrences",
                "     - src/A.java:10:1 (lines 10-12, 3 lines)",
                "     - src/B.java:20:1 (lines 20-22, 3 lines)",
            ),
            SonarDuplicatesRenderer.render(report),
        )
    }
}
