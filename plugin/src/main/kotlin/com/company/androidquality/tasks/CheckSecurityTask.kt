package com.company.androidquality.tasks

import com.company.androidquality.detekt.security.DeepLinkInjectionRule
import com.company.androidquality.detekt.security.ExportedComponentRule
import com.company.androidquality.detekt.security.HardcodedCredentialsRule
import com.company.androidquality.detekt.security.InsecureFileProviderRule
import com.company.androidquality.detekt.security.InsecureHttpUsageRule
import com.company.androidquality.detekt.security.InsecureRandomRule
import com.company.androidquality.detekt.security.LogSensitiveDataRule
import com.company.androidquality.detekt.security.UnencryptedSensitiveStorageRule
import com.company.androidquality.detekt.security.WeakCryptographyRule
import com.company.androidquality.detekt.security.WebViewJavaScriptEnabledRule
import com.company.androidquality.extension.SecurityConfig
import com.company.androidquality.report.SecurityReportGenerator
import com.company.androidquality.report.UnifiedQualityReportGenerator
import io.github.detekt.parser.KtCompiler
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Finding
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CheckSecurityTask : DefaultTask() {

    @get:Internal
    lateinit var securityConfig: SecurityConfig

    @TaskAction
    fun check() {
        if (!securityConfig.enabled) {
            logger.lifecycle("[AndroidQuality] Security check disabled, skipping.")
            return
        }

        val sourceDirs = listOf("src/main/java", "src/main/kotlin")
            .map { File(project.projectDir, it) }
            .filter { it.exists() }

        if (sourceDirs.isEmpty()) {
            logger.lifecycle("[AndroidQuality] No source directories found (src/main/java, src/main/kotlin), skipping security check.")
            return
        }

        val ktFiles = sourceDirs.flatMap { dir ->
            dir.walkTopDown().filter { it.extension == "kt" }.toList()
        }

        if (ktFiles.isEmpty()) {
            logger.lifecycle("[AndroidQuality] No Kotlin source files found.")
            return
        }

        val compiler = KtCompiler()
        val config = Config.empty
        val findings = mutableListOf<Finding>()

        ktFiles.forEach { file ->
            val sourceRoot = sourceDirs.first { file.startsWith(it) }
            val ktFile = compiler.compile(sourceRoot.toPath(), file.toPath())
            val rules = listOf(
                HardcodedCredentialsRule(config),
                InsecureHttpUsageRule(config),
                InsecureRandomRule(config),
                LogSensitiveDataRule(config),
                UnencryptedSensitiveStorageRule(config),
                WeakCryptographyRule(config),
                WebViewJavaScriptEnabledRule(config),
                ExportedComponentRule(config),
                DeepLinkInjectionRule(config),
                InsecureFileProviderRule(config)
            )
            rules.forEach { rule ->
                rule.visit(ktFile)
                findings.addAll(rule.findings)
            }
        }

        if (findings.isEmpty()) {
            logger.lifecycle("[AndroidQuality] No security violations found.")
        } else {
            findings.forEach { finding ->
                logger.warn(
                    "[AndroidQuality Security] ${finding.id}: ${finding.message} " +
                        "— ${finding.location.filePath}:${finding.location.source.line}"
                )
            }
            logger.lifecycle("[AndroidQuality] Found ${findings.size} security violation(s).")
        }

        // Always write structured findings for generateQualityReport task
        writeSecurityFindingsJson(findings)

        if (securityConfig.generateReport) {
            val reportDir = File(project.projectDir, securityConfig.reportDir)
            reportDir.mkdirs()

            val htmlFile = File(reportDir, "security-report.html")
            SecurityReportGenerator.generate(findings, htmlFile, project.name)
            logger.lifecycle("[AndroidQuality] Security report (HTML): ${htmlFile.relativeTo(project.projectDir)}")

            UnifiedQualityReportGenerator.generate(project, reportDir)
            val mdFile = File(reportDir, "quality-report.md")
            if (mdFile.exists()) {
                logger.lifecycle("[AndroidQuality] Quality report (Markdown): ${mdFile.relativeTo(project.projectDir)}")
                logger.lifecycle("[AndroidQuality] Fix with Claude Code: claude \"read ${mdFile.relativeTo(project.projectDir)} and fix all issues listed\"")
            }
        }

        if (securityConfig.failOnViolation && findings.isNotEmpty()) {
            throw GradleException(
                "[AndroidQuality] Security check failed with ${findings.size} security violation(s). See warnings above."
            )
        }
    }

    private fun writeSecurityFindingsJson(findings: List<Finding>) {
        val outputDir = File(project.layout.buildDirectory.asFile.get(), "android-quality")
        outputDir.mkdirs()
        val json = buildString {
            append("[\n")
            findings.forEachIndexed { i, f ->
                val filePath = f.location.filePath.absolutePath.toString().replace("\\", "/")
                val msg = f.message.replace("\\", "\\\\").replace("\"", "\\\"")
                append("  {")
                append("\"id\":\"${f.id}\",")
                append("\"message\":\"$msg\",")
                append("\"file\":\"$filePath\",")
                append("\"line\":${f.location.source.line},")
                append("\"severity\":\"${f.severity.name}\"")
                append("}")
                if (i < findings.size - 1) append(",")
                append("\n")
            }
            append("]")
        }
        File(outputDir, "security-findings.json").writeText(json)
    }
}
