package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class HardcodedCredentialsRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "HardcodedCredentials",
        severity = Severity.Security,
        description = "Hardcoded credential detected. Move to secure storage or environment variable.",
        debt = Debt.TWENTY_MINS
    )

    private val credentialKeywords = listOf("password", "apikey", "api_key", "secret", "token", "credential")

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val name = property.name?.lowercase() ?: return
        if (credentialKeywords.none { name.contains(it) }) return

        val initializer = property.initializer as? KtStringTemplateExpression ?: return
        // Only flag plain string literals (no template expressions like ${...})
        if (initializer.entries.size != 1) return
        val entry = initializer.entries[0] as? KtLiteralStringTemplateEntry ?: return
        if (entry.text.isBlank()) return

        report(
            CodeSmell(
                issue,
                Entity.from(property),
                "Hardcoded credential in '${property.name}'. Use BuildConfig, EncryptedSharedPreferences, or environment variables instead."
            )
        )
    }
}
