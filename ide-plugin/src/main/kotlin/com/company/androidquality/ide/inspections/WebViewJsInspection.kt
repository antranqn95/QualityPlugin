package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class WebViewJsInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "JavaScript enabled in WebView (MSTG-PLATFORM-5)"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                if (expression.left?.text?.endsWith("javaScriptEnabled") == true &&
                    expression.right?.text == "true"
                ) {
                    holder.registerProblem(
                        expression,
                        "JavaScript is enabled in WebView. This can expose the app to XSS and JavaScript injection. Only enable when strictly required. [MSTG-PLATFORM-5]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
