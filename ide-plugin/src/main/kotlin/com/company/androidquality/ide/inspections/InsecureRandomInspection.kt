package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class InsecureRandomInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Insecure random number generator (MSTG-CRYPTO-6)"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                val callee = expression.calleeExpression?.text ?: return
                if (callee.endsWith("Random") && !callee.endsWith("SecureRandom")) {
                    holder.registerProblem(
                        expression,
                        "java.util.Random is not cryptographically secure. Use java.security.SecureRandom for security-sensitive operations. [MSTG-CRYPTO-6]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
