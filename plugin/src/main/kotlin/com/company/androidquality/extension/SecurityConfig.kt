package com.company.androidquality.extension

open class SecurityConfig {
    var enabled: Boolean = true
    var failOnViolation: Boolean = false
    var generateReport: Boolean = false
    var reportDir: String = "build/reports/android-quality"
    var useDetekt: Boolean = false

    // When true (and useDetekt = true), automatically adds compose-rules 0.4.28 to detektPlugins.
    // Requires the project to use Jetpack Compose.
    var useComposeRules: Boolean = false
}
