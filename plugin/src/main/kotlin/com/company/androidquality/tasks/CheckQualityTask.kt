package com.company.androidquality.tasks

import com.company.androidquality.extension.SecurityConfig
import com.company.androidquality.report.UnifiedQualityReportGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class CheckQualityTask : DefaultTask() {

    @get:Internal
    lateinit var securityConfig: SecurityConfig

    @TaskAction
    fun run() {
        val reportDir = File(project.projectDir, securityConfig.reportDir)
        UnifiedQualityReportGenerator.generate(project, reportDir)

        val jsonFile = File(reportDir, "quality-report.json")
        val mdFile = File(reportDir, "quality-report.md")

        if (jsonFile.exists()) {
            val jsonPath = jsonFile.relativeTo(project.projectDir)
            val mdPath = mdFile.relativeTo(project.projectDir)
            logger.lifecycle("[AndroidQuality] Quality report (JSON):     $jsonPath")
            logger.lifecycle("[AndroidQuality] Quality report (Markdown): $mdPath")
            logger.lifecycle("[AndroidQuality] Fix with Claude Code: claude \"read $jsonPath and fix all issues listed\"")
        } else {
            logger.error("[AndroidQuality] quality-report.json was not created — check errors above")
        }
    }
}
