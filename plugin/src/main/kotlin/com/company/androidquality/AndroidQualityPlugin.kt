package com.company.androidquality

import com.company.androidquality.detekt.integration.DetektIntegration
import com.company.androidquality.extension.AndroidQualityExtension
import com.company.androidquality.tasks.CheckSecurityTask
import com.company.androidquality.tasks.GenerateQualityReportTask
import com.company.androidquality.tasks.GenerateTestsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "androidQuality",
            AndroidQualityExtension::class.java,
            project.objects
        )

        // Auto-detect: wire our security rules into Detekt when user already has Detekt applied
        project.plugins.withId("io.gitlab.arturbosch.detekt") {
            DetektIntegration.configure(project)
        }

        val checkSecurity = project.tasks.register("checkSecurity", CheckSecurityTask::class.java) {
            group = "Android Quality"
            description = "Check for security vulnerabilities using custom Detekt rules"
            securityConfig = extension.security
        }

        project.tasks.register("generateQualityReport", GenerateQualityReportTask::class.java) {
            group = "Android Quality"
            description = "Merge security + Detekt findings into a unified report for Claude Code"
            securityConfig = extension.security
            // Always run checkSecurity first so security-findings.json is up to date
            dependsOn(checkSecurity)
        }

        project.tasks.register("generateTests", GenerateTestsTask::class.java) {
            group = "Android Quality"
            description = "Generate JUnit tests for configured packages using AI"
            aiConfig = extension.ai
            testGenConfig = extension.testGeneration
        }

        project.afterEvaluate {
            if (extension.security.useDetekt && !project.plugins.hasPlugin("io.gitlab.arturbosch.detekt")) {
                project.logger.warn(
                    "[AndroidQuality] security.useDetekt = true but the Detekt plugin is not applied. " +
                    "Add `id(\"io.gitlab.arturbosch.detekt\") version \"1.23.7\"` to your plugins block."
                )
            }

            // autoGenOnBuild: wire generateTests into the standard build lifecycle
            if (extension.testGeneration.autoGenOnBuild) {
                project.tasks.findByName("build")?.dependsOn("generateTests")
                    ?: project.logger.warn(
                        "[AndroidQuality] testGeneration.autoGenOnBuild = true but no `build` task found in this project."
                    )
            }
        }
    }
}
