package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression

class InsecureRandomRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "InsecureRandom",
        severity = Severity.Security,
        description = "java.util.Random is not cryptographically secure. Use SecureRandom for security-sensitive operations.",
        debt = Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeText = expression.calleeExpression?.text ?: return

        if (calleeText.endsWith("Random") && !calleeText.endsWith("SecureRandom")) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "java.util.Random is not cryptographically secure. Use SecureRandom instead."
                )
            )
        }
    }
}
