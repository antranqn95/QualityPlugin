package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtBinaryExpression

class WebViewJavaScriptEnabledRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "WebViewJavaScriptEnabled",
        severity = Severity.Security,
        description = "JavaScript enabled in WebView. This can expose the app to XSS and JavaScript injection attacks.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)

        if (expression.left?.text?.endsWith("javaScriptEnabled") == true &&
            expression.right?.text == "true"
        ) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "JavaScript is enabled in WebView. This can expose the app to XSS and JavaScript injection attacks (MSTG-PLATFORM-5)."
                )
            )
        }
    }
}
