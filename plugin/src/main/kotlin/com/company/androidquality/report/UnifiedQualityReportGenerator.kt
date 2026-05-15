package com.company.androidquality.report

import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

data class QualityIssue(
    val source: String,   // "security" or "detekt"
    val rule: String,
    val file: String,     // relative path
    val line: Int
)

object UnifiedQualityReportGenerator {

    fun generate(project: Project, outputDir: File) {
        val securityIssues = readSecurityFindings(project)
        val detektIssues = readDetektFindings(project)
        val allIssues = securityIssues + detektIssues
        outputDir.mkdirs()

        File(outputDir, "quality-report.json").writeText(buildJson(allIssues))
        File(outputDir, "quality-report.md").writeText(buildMarkdown(allIssues, project.name))
    }

    private fun readSecurityFindings(project: Project): List<QualityIssue> {
        val jsonFile = File(project.layout.buildDirectory.asFile.get(), "android-quality/security-findings.json")
        if (!jsonFile.exists()) return emptyList()

        val text = jsonFile.readText().trim().removePrefix("[").removeSuffix("]").trim()
        if (text.isEmpty()) return emptyList()

        return text.split("},").mapNotNull { entry ->
            val raw = entry.trim().trimEnd('}').trimStart('{')
            val fields = parseJsonFields(raw)
            val rule = fields["id"] ?: return@mapNotNull null
            val absFile = fields["file"] ?: ""
            val relFile = try {
                File(absFile).relativeTo(project.projectDir).path.replace("\\", "/")
            } catch (_: Exception) { absFile }
            QualityIssue(
                source = "security",
                rule = rule,
                file = relFile,
                line = fields["line"]?.toIntOrNull() ?: 0
            )
        }
    }

    private fun readDetektFindings(project: Project): List<QualityIssue> {
        val xmlFile = File(project.layout.buildDirectory.asFile.get(), "reports/detekt/detekt.xml")
        if (!xmlFile.exists()) return emptyList()

        return try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
            val fileNodes = doc.getElementsByTagName("file")
            val issues = mutableListOf<QualityIssue>()

            for (i in 0 until fileNodes.length) {
                val fileNode = fileNodes.item(i) as Element
                val absPath = fileNode.getAttribute("name")
                val relPath = try {
                    File(absPath).relativeTo(project.projectDir).path.replace("\\", "/")
                } catch (_: Exception) { absPath }

                val errors = fileNode.getElementsByTagName("error")
                for (j in 0 until errors.length) {
                    val error = errors.item(j) as Element
                    issues.add(QualityIssue(
                        source = "detekt",
                        rule = error.getAttribute("source").substringAfterLast("."),
                        file = relPath,
                        line = error.getAttribute("line").toIntOrNull() ?: 0
                    ))
                }
            }
            issues
        } catch (_: Exception) { emptyList() }
    }

    // Compact JSON: one line per issue, only rule + file + line
    private fun buildJson(issues: List<QualityIssue>): String = buildString {
        appendLine("[")
        issues.forEachIndexed { i, issue ->
            val comma = if (i < issues.size - 1) "," else ""
            appendLine("  {\"rule\":\"${issue.rule}\",\"file\":\"${issue.file}\",\"line\":${issue.line}}$comma")
        }
        append("]")
    }

    // Compact markdown: bullet list grouped by source, no prose
    private fun buildMarkdown(issues: List<QualityIssue>, projectName: String): String = buildString {
        appendLine("# $projectName — ${issues.size} issues")
        appendLine()
        if (issues.isEmpty()) {
            appendLine("No issues found.")
            return@buildString
        }

        val secIssues = issues.filter { it.source == "security" }
        val detIssues = issues.filter { it.source == "detekt" }

        if (secIssues.isNotEmpty()) {
            appendLine("## Security (MASVS) — ${secIssues.size}")
            secIssues.forEach { appendLine("- ${it.rule} `${it.file}:${it.line}`") }
            appendLine()
        }
        if (detIssues.isNotEmpty()) {
            appendLine("## Detekt — ${detIssues.size}")
            detIssues.forEach { appendLine("- ${it.rule} `${it.file}:${it.line}`") }
        }
    }

    // Minimal JSON field parser — avoids external dependencies
    private fun parseJsonFields(raw: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val pattern = Regex(""""(\w+)"\s*:\s*(?:"((?:[^"\\]|\\.)*)"|(-?\d+))""")
        pattern.findAll(raw).forEach { match ->
            val key = match.groupValues[1]
            val value = if (match.groupValues[2].isNotEmpty()) match.groupValues[2] else match.groupValues[3]
            result[key] = value
        }
        return result
    }
}
