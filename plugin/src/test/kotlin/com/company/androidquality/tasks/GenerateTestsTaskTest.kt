package com.company.androidquality.tasks

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GenerateTestsTaskTest {

    @TempDir
    lateinit var projectDir: File

    private fun setupProject(apiKey: String? = null, targetPackages: List<String> = emptyList()) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """rootProject.name = "test-project" """
        )
        val apiKeyLine = if (apiKey != null) """apiKey = "$apiKey" """ else ""
        val packagesLine = if (targetPackages.isNotEmpty())
            """targetPackages = listOf(${targetPackages.joinToString { "\"$it\"" }})"""
        else ""
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("com.company.android-quality") }
            androidQuality {
                ai { $apiKeyLine }
                testGeneration { $packagesLine }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `generateTests skips gracefully when API key is null`() {
        setupProject(apiKey = null)
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateTests")
            .build()
        assertThat(result.task(":generateTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("AI_API_KEY not configured")
    }

    @Test
    fun `generateTests skips gracefully when targetPackages is empty`() {
        setupProject(apiKey = "fake-key", targetPackages = emptyList())
        projectDir.resolve("src/main/java").mkdirs()
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateTests")
            .build()
        assertThat(result.task(":generateTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("No classes found")
    }

    @Test
    fun `generateTests skips gracefully when target package directory does not exist`() {
        setupProject(apiKey = "fake-key", targetPackages = listOf("com.example.nonexistent"))
        projectDir.resolve("src/main/java").mkdirs()
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateTests")
            .build()
        assertThat(result.task(":generateTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("No classes found")
    }

    @Test
    fun `generateTests skips existing test file when skipExisting is true`() {
        val pkg = "com.example.app"
        setupProject(apiKey = "fake-key", targetPackages = listOf(pkg))

        val srcDir = projectDir.resolve("src/main/java/com/example/app").also { it.mkdirs() }
        srcDir.resolve("MyClass.kt").writeText("""
            package com.example.app
            class MyClass { fun doWork() = "done" }
        """.trimIndent())

        val testDir = projectDir.resolve("src/test/java/com/example/app").also { it.mkdirs() }
        testDir.resolve("MyClassTest.kt").writeText("package com.example.app\n// existing test")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateTests")
            .build()

        assertThat(result.task(":generateTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Skipped (exists)")
        assertThat(result.output).contains("0 generated, 1 skipped")
    }

    @Test
    fun `generateTests respects custom outputDir from testGeneration config`() {
        val pkg = "com.example.app"
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "test-project" """)
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins { id("com.company.android-quality") }
            androidQuality {
                ai { apiKey = "fake-key" }
                testGeneration {
                    targetPackages = listOf("$pkg")
                    outputDir = "src/androidTest/java"
                    skipExisting = true
                }
            }
        """.trimIndent())

        val srcDir = projectDir.resolve("src/main/java/com/example/app").also { it.mkdirs() }
        srcDir.resolve("MyClass.kt").writeText("""
            package com.example.app
            class MyClass { fun doWork() = "done" }
        """.trimIndent())

        val customTestDir = projectDir.resolve("src/androidTest/java/com/example/app").also { it.mkdirs() }
        customTestDir.resolve("MyClassTest.kt").writeText("package com.example.app\n// existing test")

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateTests")
            .build()

        assertThat(result.task(":generateTests")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("Skipped (exists)")
        assertThat(result.output).contains("src/androidTest/java")
    }
}
