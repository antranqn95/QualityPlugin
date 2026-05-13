package com.company.androidquality.extension

open class SecurityConfig {
    var enabled: Boolean = true
    var failOnViolation: Boolean = false
    var generateReport: Boolean = false
    var reportDir: String = "build/reports/android-quality"
    /**
     * When true, applies the Detekt Gradle plugin (if not already present) and wires
     * all 10 MASVS security rules into `./gradlew detekt`.
     * Also activates automatically when `io.gitlab.arturbosch.detekt` is already applied.
     */
    var useDetekt: Boolean = false
}
