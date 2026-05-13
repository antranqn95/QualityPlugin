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
        UnifiedQualityReportGenerator.generate(project, reportDir)

        val mdPath = File(reportDir, "quality-report.md").relativeTo(project.projectDir)
        val jsonPath = File(reportDir, "quality-report.json").relativeTo(project.projectDir)
        logger.lifecycle("[AndroidQuality] Quality report (Markdown): $mdPath")
        logger.lifecycle("[AndroidQuality] Quality report (JSON):     $jsonPath")
        logger.lifecycle("[AndroidQuality] Fix with Claude Code: claude \"read $mdPath and fix all issues listed\"")
    }
}
