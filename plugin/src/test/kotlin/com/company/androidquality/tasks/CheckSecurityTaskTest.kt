package com.company.androidquality.tasks

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CheckSecurityTaskTest {

    @TempDir
    lateinit var projectDir: File

    private fun setupProject(sourceContent: String = "", failOnViolation: Boolean = false) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-project" """
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.company.android-quality") }
            androidQuality {
                security {
                    failOnViolation = $failOnViolation
                }
            }
            """.trimIndent()
        )
        if (sourceContent.isNotBlank()) {
            val srcDir = projectDir.resolve("src/main/java").also { it.mkdirs() }
            srcDir.resolve("Sample.kt").writeText(sourceContent)
        }
    }

    @Test
    fun `plugin applies and checkSecurity task exists`() {
        setupProject()
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("tasks", "--group=Android Quality")
            .build()
        assertThat(result.output).contains("checkSecurity")
        assertThat(result.output).contains("generateTests")
    }

    @Test
    fun `checkSecurity passes with no source files`() {
        setupProject()
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()
        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `checkSecurity warns on hardcoded password when failOnViolation is false`() {
        setupProject("""val password = "hunter2" """, failOnViolation = false)
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()
        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("HardcodedCredentials")
    }

    @Test
    fun `checkSecurity fails build when failOnViolation is true`() {
        setupProject("""val password = "hunter2" """, failOnViolation = true)
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .buildAndFail()
        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("security violation")
    }

    @Test
    fun `checkSecurity skips when security is disabled`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-project" """
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.company.android-quality") }
            androidQuality {
                security {
                    enabled = false
                    failOnViolation = true
                }
            }
            """.trimIndent()
        )
        val srcDir = projectDir.resolve("src/main/java").also { it.mkdirs() }
        srcDir.resolve("Sample.kt").writeText("""val password = "should-not-be-checked" """)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Security check disabled")
    }

    @Test
    fun `checkSecurity skips gracefully when source directory does not exist`() {
        setupProject(sourceContent = "")
        // sourceContent blank means src/main/java is not created

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }

    @Test
    fun `checkSecurity detects WeakCryptography violation`() {
        setupProject(
            sourceContent = """val md = MessageDigest.getInstance("MD5")""",
            failOnViolation = false,
        )
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("WeakCryptography")
    }

    @Test
    fun `checkSecurity detects LogSensitiveData violation`() {
        setupProject(
            sourceContent = """fun f() { Log.d("TAG", "password=${'$'}userPassword") }""",
            failOnViolation = false,
        )
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("LogSensitiveData")
    }

    @Test
    fun `checkSecurity detects WebViewJavaScriptEnabled violation`() {
        setupProject(
            sourceContent = """fun f() { webView.settings.javaScriptEnabled = true }""",
            failOnViolation = false,
        )
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("WebViewJavaScriptEnabled")
    }

    @Test
    fun `checkSecurity detects InsecureRandom violation`() {
        setupProject(
            sourceContent = """fun f() { val r = Random() }""",
            failOnViolation = false,
        )
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.task(":checkSecurity")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("InsecureRandom")
    }

    @Test
    fun `checkSecurity reports all violations from multiple rules in one file`() {
        setupProject(
            sourceContent = """
                val password = "hunter2"
                val md = MessageDigest.getInstance("MD5")
            """.trimIndent(),
            failOnViolation = false,
        )
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("checkSecurity")
            .build()

        assertThat(result.output).contains("HardcodedCredentials")
        assertThat(result.output).contains("WeakCryptography")
    }
}
