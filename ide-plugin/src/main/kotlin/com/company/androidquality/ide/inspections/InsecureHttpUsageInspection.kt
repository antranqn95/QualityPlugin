package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class InsecureHttpUsageInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Insecure HTTP URL (MSTG-NETWORK-1)"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
                val text = expression.entries
                    .filterIsInstance<KtLiteralStringTemplateEntry>()
                    .joinToString("") { it.text }

                if (text.startsWith("http://")) {
                    holder.registerProblem(
                        expression,
                        "Insecure HTTP URL '$text'. Use HTTPS to protect data in transit. [MSTG-NETWORK-1]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
