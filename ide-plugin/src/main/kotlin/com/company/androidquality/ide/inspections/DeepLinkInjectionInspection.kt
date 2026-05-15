package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class DeepLinkInjectionInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Deep link injection (MSTG-PLATFORM-3)"

    private val sourceMethods = setOf(
        "getQueryParameter", "getQueryParameterNames",
        "getStringExtra", "getStringArrayExtra",
        "getData", "getDataString", "getAction", "getScheme"
    )
    private val sinkMethods = setOf(
        "loadUrl", "loadData", "loadDataWithBaseURL",
        "startActivity", "startActivityForResult", "startService",
        "sendBroadcast", "sendOrderedBroadcast"
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
                val method = selectorCall.calleeExpression?.text ?: return
                if (method !in sinkMethods) return

                val argText = selectorCall.valueArguments.joinToString { it.text }
                if (sourceMethods.any { argText.contains(it) }) {
                    holder.registerProblem(
                        expression,
                        "Unvalidated deep link or Intent data passed to '$method'. " +
                            "Validate and sanitize URI parameters and Intent extras before use. [MSTG-PLATFORM-3]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
