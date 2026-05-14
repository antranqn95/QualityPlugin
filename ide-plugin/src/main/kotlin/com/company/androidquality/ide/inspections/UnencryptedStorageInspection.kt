package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class UnencryptedStorageInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Unencrypted sensitive data in SharedPreferences (MSTG-STORAGE-1)"

    private val sensitiveKeywords = listOf("password", "passwd", "token", "secret", "apikey", "api_key", "credential", "auth")
    private val putMethods = setOf("putString", "putStringSet")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
                val method = selectorCall.calleeExpression?.text ?: return
                if (method !in putMethods) return

                val keyArg = selectorCall.valueArguments.firstOrNull()
                    ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

                val keyText = keyArg.entries
                    .filterIsInstance<KtLiteralStringTemplateEntry>()
                    .joinToString("") { it.text }
                    .lowercase()

                if (sensitiveKeywords.any { keyText.contains(it) }) {
                    holder.registerProblem(
                        expression,
                        "Sensitive key '$keyText' stored in plain SharedPreferences. Use EncryptedSharedPreferences instead. [MSTG-STORAGE-1]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
