package com.company.androidquality.tasks

import com.company.androidquality.extension.SecurityConfig
import com.company.androidquality.report.UnifiedQualityReportGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateQualityReportTask : DefaultTask() {

    @get:Internal
    lateinit var securityConfig: SecurityConfig

    @TaskAction
    fun generate() {
        val reportDir = File(project.projectDir, securityConfig.reportDir)

        try {
            UnifiedQualityReportGenerator.generate(project, reportDir)
        } catch (e: Exception) {
            logger.error("[AndroidQuality] Failed to generate quality report: ${e.message}")
            logger.error("[AndroidQuality] Run with --info for full stacktrace")
            throw e
        }

        val mdFile  = File(reportDir, "quality-report.md")
        val jsonFile = File(reportDir, "quality-report.json")

        if (mdFile.exists()) {
            val mdPath   = mdFile.relativeTo(project.projectDir)
            val jsonPath = jsonFile.relativeTo(project.projectDir)
            logger.lifecycle("[AndroidQuality] Quality report (Markdown): $mdPath")
            logger.lifecycle("[AndroidQuality] Quality report (JSON):     $jsonPath")
            logger.lifecycle("[AndroidQuality] Fix with Claude Code: claude \"read $mdPath and fix all issues listed\"")
        } else {
            logger.error("[AndroidQuality] quality-report.md was not created — check errors above")
        }
    }
}
