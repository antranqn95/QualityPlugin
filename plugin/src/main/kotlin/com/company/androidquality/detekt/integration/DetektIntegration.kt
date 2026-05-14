package com.company.androidquality.detekt.integration

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import java.io.File

/**
 * Wires our 10 MASVS security rules into the project's existing Detekt setup.
 *
 * When active, `./gradlew detekt` runs standard Detekt code-quality checks
 * PLUS all our security rules — with findings grouped under "android-security" in the report.
 *
 * Activation (either triggers the wiring):
 *   1. Automatic — user has both `io.gitlab.arturbosch.detekt` and `com.company.android-quality` applied.
 *   2. Explicit   — `security { useDetekt = true }` in the androidQuality block.
 */
internal object DetektIntegration {

    fun configure(project: Project) {
        addSecurityRulesToDetektClasspath(project)
        enableSecurityRulesInConfig(project)
        project.logger.lifecycle(
            "[AndroidQuality] Detekt integration active — 10 MASVS security rules added to `./gradlew detekt`"
        )
    }

    /**
     * Adds our plugin JAR itself to the `detektPlugins` configuration.
     * Detekt uses ServiceLoader to discover RuleSetProvider in each plugin JAR,
     * so our SecurityRuleSetProvider is picked up automatically.
     */
    private fun addSecurityRulesToDetektClasspath(project: Project) {
        val pluginJar = File(
            DetektIntegration::class.java.protectionDomain.codeSource.location.toURI()
        )
        project.dependencies.add("detektPlugins", project.files(pluginJar))
    }

    /**
     * Generates a YAML snippet that enables all 10 security rules and registers it
     * with the Detekt extension config. Uses buildUponDefaultConfig = true so standard
     * Detekt rules are NOT replaced — our security rules are added on top.
     */
    private fun enableSecurityRulesInConfig(project: Project) {
        val configFile = generateSecurityConfigFile(project)

        val ext = project.extensions.getByType(DetektExtension::class.java)
        ext.buildUponDefaultConfig = true
        ext.config.from(configFile)
    }

    private fun generateSecurityConfigFile(project: Project): File {
        val file = File(
            project.layout.buildDirectory.asFile.get(),
            "android-quality/detekt-android-security.yml"
        )
        file.parentFile.mkdirs()
        file.writeText(SECURITY_RULES_YAML)
        return file
    }

    // All 10 rules active by default. Users can override in their own detekt.yml.
    private val SECURITY_RULES_YAML = """
        # Android Quality Plugin — OWASP MASVS Security Rules
        # Auto-generated. Override individual rules in your project's detekt.yml.
        android-security:
          HardcodedCredentials:
            active: true
          InsecureHttpUsage:
            active: true
          WeakCryptography:
            active: true
          LogSensitiveData:
            active: true
          UnencryptedSensitiveStorage:
            active: true
          WebViewJavaScriptEnabled:
            active: true
          InsecureRandom:
            active: true
          ExportedComponent:
            active: true
          DeepLinkInjection:
            active: true
          InsecureFileProvider:
            active: true
    """.trimIndent()
}
