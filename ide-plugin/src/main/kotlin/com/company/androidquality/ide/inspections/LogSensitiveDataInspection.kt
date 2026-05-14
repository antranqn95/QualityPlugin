package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class LogSensitiveDataInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Sensitive data in log statements (MSTG-STORAGE-3)"

    private val logMethods = setOf("v", "d", "i", "w", "e", "wtf")
    private val printMethods = setOf("println", "print")
    private val sensitiveKeywords = listOf(
        "password", "passwd", "apikey", "api_key", "secret", "token",
        "credential", "private_key", "privatekey", "accesskey", "auth"
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val receiver = expression.receiverExpression.text
                if (receiver != "Log" && !receiver.endsWith(".Log")) return

                val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
                val method = selectorCall.calleeExpression?.text ?: return
                if (method !in logMethods) return

                val messageArg = selectorCall.valueArguments.getOrNull(1)
                    ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

                val messageText = messageArg.entries.joinToString("") { it.text }.lowercase()
                if (sensitiveKeywords.any { messageText.contains(it) }) {
                    holder.registerProblem(
                        expression,
                        "Potential sensitive data in Log.$method(). Passwords, tokens, and keys must not appear in logs. [MSTG-STORAGE-3]",
                        ProblemHighlightType.WARNING
                    )
                }
            }

            override fun visitCallExpression(expression: KtCallExpression) {
                val method = expression.calleeExpression?.text ?: return
                if (method !in printMethods) return

                val messageArg = expression.valueArguments.getOrNull(0)
                    ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

                val messageText = messageArg.entries.joinToString("") { it.text }.lowercase()
                if (sensitiveKeywords.any { messageText.contains(it) }) {
                    holder.registerProblem(
                        expression,
                        "Potential sensitive data in $method(). Passwords, tokens, and keys must not appear in logs. [MSTG-STORAGE-3]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
