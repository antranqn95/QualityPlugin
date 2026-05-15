package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class DeepLinkInjectionRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "DeepLinkInjection",
        severity = Severity.Security,
        description = "Unvalidated deep link or intent data passed to a sensitive sink may allow injection attacks.",
        debt = Debt.TWENTY_MINS
    )

    // Sources: methods that read untrusted external input
    private val sourceMethods = setOf(
        "getQueryParameter", "getQueryParameterNames",
        "getStringExtra", "getStringArrayExtra",
        "getData", "getDataString",
        "getAction", "getScheme"
    )

    // Sinks: methods that consume untrusted data in potentially dangerous ways
    private val sinkMethods = setOf(
        "loadUrl", "loadData", "loadDataWithBaseURL",
        "startActivity", "startActivityForResult", "startService",
        "sendBroadcast", "sendOrderedBroadcast",
        "openConnection", "openStream"
    )

    private val sourcesFoundInFunction = mutableSetOf<String>()

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
        val methodName = selectorCall.calleeExpression?.text ?: return

        if (methodName in sourceMethods) {
            // Track variable name if this is part of an assignment (e.g. val url = uri.getQueryParameter(...))
            val parent = expression.parent
            val rawLeft = parent?.text?.substringBefore("=")?.trim() ?: ""
            val varName = rawLeft
                .removePrefix("val ").removePrefix("var ")
                .trim()
                .takeIf { it.isNotBlank() && !it.contains(" ") && !it.contains(".") }
            if (varName != null) sourcesFoundInFunction.add(varName)
        }

        if (methodName in sinkMethods) {
            val argText = selectorCall.valueArguments.joinToString { it.text }
            val usesExternalInput = sourceMethods.any { argText.contains(it) } ||
                sourcesFoundInFunction.any { argText.contains(it) }

            if (usesExternalInput) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        "Unvalidated external input (from deep link or Intent) passed to '$methodName'. " +
                            "Validate and sanitize all URI parameters and Intent extras before use (MSTG-PLATFORM-3)."
                    )
                )
            }
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val methodName = expression.calleeExpression?.text ?: return
        if (methodName !in sinkMethods) return

        val argText = expression.valueArguments.joinToString { it.text }
        val usesExternalInput = sourceMethods.any { argText.contains(it) } ||
            sourcesFoundInFunction.any { argText.contains(it) }

        if (usesExternalInput) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Unvalidated external input passed to '$methodName'. " +
                        "Validate and sanitize all URI parameters and Intent extras before use (MSTG-PLATFORM-3)."
                )
            )
        }
    }
}
