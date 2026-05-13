package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class UnencryptedSensitiveStorageRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "UnencryptedSensitiveStorage",
        severity = Severity.Security,
        description = "Sensitive data stored in plain SharedPreferences. Use EncryptedSharedPreferences.",
        debt = Debt.TWENTY_MINS
    )

    private val prefsMethods = setOf(
        "putString", "putInt", "putBoolean", "putFloat", "putLong",
        "getString", "getInt", "getBoolean", "getFloat", "getLong"
    )
    private val sensitiveKeywords = listOf("password", "token", "secret", "credential")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        // Get the callee expression text
        val calleeExpr = expression.calleeExpression ?: return
        val calleeText = calleeExpr.text

        // Extract just the method name (last identifier)
        val methodName = calleeText.split(".").last()

        // Check if this is one of the methods we care about
        if (methodName !in prefsMethods) {
            return
        }

        // Get the first argument
        val firstArg = expression.valueArguments.firstOrNull() ?: return
        val argExpr = firstArg.getArgumentExpression() ?: return

        // Check if it's a string template
        if (argExpr !is KtStringTemplateExpression) {
            return
        }

        // Extract the key text from the string
        val keyText = argExpr.entries
            .filterIsInstance<KtLiteralStringTemplateEntry>()
            .joinToString("") { it.text }
            .lowercase()

        // Check if the key contains any sensitive keywords
        if (sensitiveKeywords.any { keyword -> keyText.contains(keyword) }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Sensitive key '$keyText' stored in plain SharedPreferences. Use EncryptedSharedPreferences from Jetpack Security."
                )
            )
        }
    }
}
