package com.company.androidquality.extension

open class TestGenerationConfig {
    var targetPackages: List<String> = emptyList()
    var testFramework: String = "junit4"
    var outputDir: String = "src/test/java"
    var skipExisting: Boolean = true
    var useCache: Boolean = true
    /** When true, `generateTests` runs automatically as part of the `build` lifecycle. */
    var autoGenOnBuild: Boolean = false
}
