package com.company.androidquality.ai

import java.io.File

data class ClassInfo(
    val className: String,
    val packageName: String,
    val packagePath: String,
    val source: String,
    val hasFlowTypes: Boolean = false
)

class KotlinCodeAnalyzer {

    private val flowTypePattern = Regex(
        """\b(?:Mutable)?(?:State|Shared)Flow\s*<|:\s*Flow\s*<"""
    )

    fun analyze(directory: File): List<ClassInfo> {
        if (!directory.exists()) return emptyList()
        return directory.walkTopDown()
            .filter { it.extension == "kt" }
            .mapNotNull { parseFile(it) }
            .toList()
    }

    private fun parseFile(file: File): ClassInfo? {
        val source = file.readText()

        val packageName = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)
            .find(source)?.groupValues?.get(1) ?: return null

        // Match first non-private, non-annotation class declaration
        val className = Regex(
            """(?:^|(?<=\n))\s*(?:public\s+|internal\s+)?(?:open\s+|abstract\s+|data\s+|sealed\s+)?class\s+(\w+)"""
        ).find(source)?.groupValues?.get(1) ?: return null

        return ClassInfo(
            className = className,
            packageName = packageName,
            packagePath = packageName.replace('.', '/'),
            source = source,
            hasFlowTypes = flowTypePattern.containsMatchIn(source)
        )
    }
}
