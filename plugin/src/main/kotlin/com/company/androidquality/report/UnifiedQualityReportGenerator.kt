package com.company.androidquality.report

import org.gradle.api.Project
import org.w3c.dom.Element
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

data class QualityIssue(
    val id: String,          // "SEC-001", "DET-001"
    val source: String,      // "MASVS" or "Detekt"
    val rule: String,
    val severity: String,
    val file: String,        // relative path
    val line: Int,
    val message: String,
    val fixHint: String
)

object UnifiedQualityReportGenerator {

    private val securityFixHints = mapOf(
        "HardcodedCredentials" to "Move to `gradle.properties`, environment variable, or Android Keystore. Never commit credentials to source control.",
        "InsecureHttpUsage" to "Replace `http://` with `https://`. Check `android:usesCleartextTraffic` in AndroidManifest.",
        "WeakCryptography" to "Use AES-256-GCM or ChaCha20-Poly1305. Replace MD5/SHA-1 with SHA-256 or stronger.",
        "LogSensitiveData" to "Remove sensitive values from logs. Strip PII in a release-mode log wrapper.",
        "UnencryptedSensitiveStorage" to "Use `EncryptedSharedPreferences` from Jetpack Security (`androidx.security:security-crypto`).",
        "WebViewJavaScriptEnabled" to "Disable JavaScript unless required. Validate all URLs before loading. Implement `shouldOverrideUrlLoading` safely.",
        "InsecureRandom" to "Replace `java.util.Random` / `kotlin.random.Random` with `java.security.SecureRandom` for security operations.",
        "ExportedComponent" to "Add `android:permission` in the manifest, or call `checkCallingPermission()` / `enforcePermission()` at runtime.",
        "DeepLinkInjection" to "Validate and sanitize all URI/Intent parameters before passing to sensitive sinks (WebView, startActivity, etc.).",
        "InsecureFileProvider" to "Replace `Uri.fromFile()` with `FileProvider.getUriForFile()`. Avoid direct external storage access."
    )

    fun generate(project: Project, outputDir: File) {
        val securityIssues = readSecurityFindings(project)
        val detektIssues = readDetektFindings(project)

        val allIssues = securityIssues + detektIssues
        outputDir.mkdirs()

        val mdFile = File(outputDir, "quality-report.md")
        mdFile.writeText(buildMarkdown(allIssues, project.name, securityIssues.size, detektIssues.size))

        val jsonFile = File(outputDir, "quality-report.json")
        jsonFile.writeText(buildJson(allIssues))
    }

    private fun readSecurityFindings(project: Project): List<QualityIssue> {
        val jsonFile = File(project.layout.buildDirectory.asFile.get(), "android-quality/security-findings.json")
        if (!jsonFile.exists()) return emptyList()

        val text = jsonFile.readText().trim().removePrefix("[").removeSuffix("]").trim()
        if (text.isEmpty()) return emptyList()

        var idx = 1
        return text.split("},").mapNotNull { entry ->
            val raw = entry.trim().trimEnd('}').trimStart('{')
            val fields = parseJsonFields(raw)
            val id = fields["id"] ?: return@mapNotNull null
            val file = fields["file"] ?: ""
            val relFile = try {
                File(file).relativeTo(project.projectDir).path.replace("\\", "/")
            } catch (_: Exception) { file }
            QualityIssue(
                id = "SEC-${idx.toString().padStart(3, '0')}",
                source = "MASVS",
                rule = id,
                severity = fields["severity"] ?: "Security",
                file = relFile,
                line = fields["line"]?.toIntOrNull() ?: 0,
                message = fields["message"] ?: "",
                fixHint = securityFixHints[id] ?: "Review and remediate according to OWASP MASVS guidelines."
            ).also { idx++ }
        }
    }

    private fun readDetektFindings(project: Project): List<QualityIssue> {
        val xmlFile = File(project.layout.buildDirectory.asFile.get(), "reports/detekt/detekt.xml")
        if (!xmlFile.exists()) return emptyList()

        return try {
            val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = db.parse(xmlFile)
            val fileNodes = doc.getElementsByTagName("file")
            val issues = mutableListOf<QualityIssue>()
            var idx = 1

            for (i in 0 until fileNodes.length) {
                val fileNode = fileNodes.item(i) as Element
                val absPath = fileNode.getAttribute("name")
                val relPath = try {
                    File(absPath).relativeTo(project.projectDir).path.replace("\\", "/")
                } catch (_: Exception) { absPath }

                val errors = fileNode.getElementsByTagName("error")
                for (j in 0 until errors.length) {
                    val error = errors.item(j) as Element
                    val source = error.getAttribute("source") // "detekt.RuleSetId.RuleName"
                    val ruleName = source.substringAfterLast(".")
                    issues.add(QualityIssue(
                        id = "DET-${idx.toString().padStart(3, '0')}",
                        source = "Detekt",
                        rule = ruleName,
                        severity = error.getAttribute("severity").uppercase(),
                        file = relPath,
                        line = error.getAttribute("line").toIntOrNull() ?: 0,
                        message = error.getAttribute("message"),
                        fixHint = "See Detekt rule documentation: https://detekt.dev/docs/rules/${source.substringAfter(".").substringBefore(".").lowercase()}"
                    ).also { idx++ })
                }
            }
            issues
        } catch (_: Exception) { emptyList() }
    }

    private fun buildMarkdown(
        issues: List<QualityIssue>,
        projectName: String,
        secCount: Int,
        detCount: Int
    ): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val sb = StringBuilder()

        sb.appendLine("# Quality Issues Report — $projectName")
        sb.appendLine()
        sb.appendLine("> **Generated:** $timestamp")
        sb.appendLine("> **Refresh:** `./gradlew checkSecurity generateQualityReport`")
        sb.appendLine("> **Fix with Claude Code:** `claude \"read build/reports/android-quality/quality-report.md and fix all issues listed\"`")
        sb.appendLine()
        sb.appendLine("## Summary")
        sb.appendLine()
        sb.appendLine("| Source | Issues |")
        sb.appendLine("|--------|--------|")
        sb.appendLine("| Security (MASVS) | $secCount |")
        sb.appendLine("| Detekt | $detCount |")
        sb.appendLine("| **Total** | **${issues.size}** |")
        sb.appendLine()

        if (issues.isEmpty()) {
            sb.appendLine("No issues found. Great work!")
            return sb.toString()
        }

        val secIssues = issues.filter { it.source == "MASVS" }
        val detIssues = issues.filter { it.source == "Detekt" }

        if (secIssues.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## Security Issues (MASVS)")
            sb.appendLine()
            secIssues.forEach { issue ->
                sb.appendLine("### [${issue.id}] ${issue.rule} — ${issue.severity}")
                sb.appendLine("- **File:** `${issue.file}:${issue.line}`")
                sb.appendLine("- **Rule:** ${issue.rule} (OWASP MASVS)")
                sb.appendLine("- **Message:** ${issue.message}")
                sb.appendLine("- **Fix:** ${issue.fixHint}")
                sb.appendLine()
            }
        }

        if (detIssues.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine()
            sb.appendLine("## Detekt Issues")
            sb.appendLine()
            detIssues.forEach { issue ->
                sb.appendLine("### [${issue.id}] ${issue.rule} — ${issue.severity}")
                sb.appendLine("- **File:** `${issue.file}:${issue.line}`")
                sb.appendLine("- **Rule:** ${issue.rule}")
                sb.appendLine("- **Message:** ${issue.message}")
                sb.appendLine("- **Fix:** ${issue.fixHint}")
                sb.appendLine()
            }
        }

        return sb.toString()
    }

    private fun buildJson(issues: List<QualityIssue>): String = buildString {
        append("[\n")
        issues.forEachIndexed { i, issue ->
            val msg = issue.message.replace("\\", "\\\\").replace("\"", "\\\"")
            val hint = issue.fixHint.replace("\\", "\\\\").replace("\"", "\\\"")
            append("  {")
            append("\"id\":\"${issue.id}\",")
            append("\"source\":\"${issue.source}\",")
            append("\"rule\":\"${issue.rule}\",")
            append("\"severity\":\"${issue.severity}\",")
            append("\"file\":\"${issue.file}\",")
            append("\"line\":${issue.line},")
            append("\"message\":\"$msg\",")
            append("\"fixHint\":\"$hint\"")
            append("}")
            if (i < issues.size - 1) append(",")
            append("\n")
        }
        append("]")
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
