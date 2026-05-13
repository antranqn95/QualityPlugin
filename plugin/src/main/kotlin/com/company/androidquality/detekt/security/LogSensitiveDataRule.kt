package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class LogSensitiveDataRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "LogSensitiveData",
        severity = Severity.Security,
        description = "Sensitive data may be written to logs. Remove or redact before release.",
        debt = Debt.TEN_MINS
    )

    // Android Log method names (without the "Log." prefix)
    private val androidLogMethodNames = setOf("v", "d", "i", "w", "e", "wtf")
    // println / print: message is index 0
    private val printMethods = setOf("println", "print")

    private val sensitiveKeywords = listOf(
        "password", "passwd", "apikey", "api_key", "secret", "token",
        "credential", "private_key", "privatekey", "accesskey", "auth"
    )

    // Handle Log.d(TAG, "message"), Log.e(TAG, "message"), etc.
    // PSI structure: KtDotQualifiedExpression { receiver="Log", selector=KtCallExpression { callee="d", args=[TAG, "message"] } }
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val receiverText = expression.receiverExpression.text
        // Only handle android.util.Log (receiver is "Log" or ends with ".Log")
        if (receiverText != "Log" && !receiverText.endsWith(".Log")) return

        val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
        val methodName = selectorCall.calleeExpression?.text ?: return
        if (methodName !in androidLogMethodNames) return

        val args = selectorCall.valueArguments
        // Log methods: (tag, message) → message is at index 1
        val messageArg = args.getOrNull(1)
            ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

        checkMessageAndReport(messageArg, expression)
    }

    // Handle println("message") and print("message")
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callText = expression.calleeExpression?.text ?: return
        if (callText !in printMethods) return

        val messageArg = expression.valueArguments.getOrNull(0)
            ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

        checkMessageAndReport(messageArg, expression)
    }

    private fun checkMessageAndReport(
        messageArg: KtStringTemplateExpression,
        reportTarget: org.jetbrains.kotlin.psi.KtElement
    ) {
        // Extract full template text including interpolated variable names ($var or ${expr})
        val messageText = messageArg.entries.joinToString("") { it.text }.lowercase()

        if (sensitiveKeywords.any { messageText.contains(it) }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(reportTarget),
                    "Potential sensitive data in log statement. Ensure passwords, tokens, and keys are not logged in production."
                )
            )
        }
    }
}
