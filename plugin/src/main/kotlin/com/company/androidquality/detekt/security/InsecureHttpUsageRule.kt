package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class InsecureHttpUsageRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "InsecureHttpUsage",
        severity = Severity.Security,
        description = "Insecure HTTP URL detected. Use HTTPS to prevent man-in-the-middle attacks.",
        debt = Debt.FIVE_MINS
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val firstEntry = expression.entries.firstOrNull() as? KtLiteralStringTemplateEntry ?: return
        if (firstEntry.text.startsWith("http://")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Insecure HTTP URL '${firstEntry.text}...'. Replace with HTTPS."
                )
            )
        }
    }
}
