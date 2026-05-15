package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class WeakCryptographyInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Weak cryptographic algorithm (MSTG-CRYPTO-4)"

    private val weakAlgorithms = listOf("md5", "sha-1", "sha1", "des", "rc4", "rc2", "blowfish")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val callText = expression.calleeExpression?.text ?: return
                if (!callText.endsWith("getInstance")) return

                val firstArg = expression.valueArguments.firstOrNull()
                    ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

                val algorithmText = firstArg.entries
                    .filterIsInstance<KtLiteralStringTemplateEntry>()
                    .joinToString("") { it.text }
                    .lowercase()

                val isWeak = weakAlgorithms.any { algorithmText == it || algorithmText.startsWith("$it/") } ||
                    "/ecb/" in algorithmText

                if (isWeak) {
                    holder.registerProblem(
                        expression,
                        "Weak algorithm '$algorithmText'. Use SHA-256 for hashing or AES/GCM/NoPadding for encryption. [MSTG-CRYPTO-4]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
