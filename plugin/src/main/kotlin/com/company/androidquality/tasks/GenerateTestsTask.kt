package com.company.androidquality.tasks

import com.company.androidquality.ai.AITestGenerator
import com.company.androidquality.ai.KotlinCodeAnalyzer
import com.company.androidquality.ai.TestGenCache
import com.company.androidquality.extension.AiConfig
import com.company.androidquality.extension.TestGenerationConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateTestsTask : DefaultTask() {

    @get:Internal
    lateinit var aiConfig: AiConfig

    @get:Internal
    lateinit var testGenConfig: TestGenerationConfig

    @TaskAction
    fun generate() {
        if (aiConfig.apiKey.isNullOrBlank()) {
            logger.lifecycle("[AndroidQuality] AI_API_KEY not configured, skipping test generation.")
            logger.lifecycle("[AndroidQuality] Add AI_API_KEY=<your-key> to ~/.gradle/gradle.properties to enable.")
            return
        }

        val sourceDirs = listOf("src/main/java", "src/main/kotlin")
            .map { File(project.projectDir, it) }
            .filter { it.exists() }

        if (sourceDirs.isEmpty()) {
            logger.lifecycle("[AndroidQuality] No source directories found (src/main/java, src/main/kotlin).")
            return
        }

        val analyzer = KotlinCodeAnalyzer()
        val classes = testGenConfig.targetPackages.flatMap { pkg ->
            val pkgPath = pkg.replace('.', '/')
            sourceDirs
                .map { File(it, pkgPath) }
                .filter { it.exists() }
                .flatMap { analyzer.analyze(it) }
        }

        if (classes.isEmpty()) {
            logger.lifecycle("[AndroidQuality] No classes found in configured packages. Check testGeneration.targetPackages.")
            return
        }

        val generator = AITestGenerator(aiConfig.apiKey!!, aiConfig.model)
        val outputDirFile = File(project.projectDir, testGenConfig.outputDir)
        val cache = if (testGenConfig.useCache) {
            TestGenCache(File(project.layout.buildDirectory.asFile.get(), "android-quality/test-gen-cache.json"))
        } else null

        var generated = 0
        var skipped = 0
        var cached = 0

        classes.forEach { classInfo ->
            val outputFile = File(outputDirFile, "${classInfo.packagePath}/${classInfo.className}Test.kt")

            if (outputFile.exists() && testGenConfig.skipExisting) {
                logger.lifecycle("[AndroidQuality] Skipped (exists): ${outputFile.relativeTo(project.projectDir)}")
                skipped++
                return@forEach
            }

            if (cache != null && outputFile.exists() && cache.isUpToDate(classInfo.className, classInfo.source)) {
                logger.lifecycle("[AndroidQuality] Cached (source unchanged): ${classInfo.className}")
                cached++
                return@forEach
            }

            try {
                logger.lifecycle("[AndroidQuality] Generating tests for ${classInfo.className}...")
                val testContent = generator.generate(classInfo, testGenConfig.testFramework)
                outputFile.parentFile.mkdirs()
                outputFile.writeText(testContent)
                cache?.put(classInfo.className, classInfo.source)
                logger.lifecycle("[AndroidQuality] Generated: ${outputFile.relativeTo(project.projectDir)}")
                generated++
            } catch (e: Exception) {
                logger.warn("[AndroidQuality] Failed to generate test for ${classInfo.className}: ${e.message}")
            }
        }

        val summary = buildString {
            append("[AndroidQuality] Done: $generated generated")
            if (cached > 0) append(", $cached cached")
            if (skipped > 0) append(", $skipped skipped")
        }
        logger.lifecycle(summary)
    }
}
