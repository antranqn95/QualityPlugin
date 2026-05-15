package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class InsecureFileProviderInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Insecure file access (MSTG-STORAGE-2)"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                val receiver = expression.receiverExpression.text
                val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
                val method = selectorCall.calleeExpression?.text ?: return

                if ((receiver == "Uri" || receiver.endsWith(".Uri")) && method == "fromFile") {
                    holder.registerProblem(
                        expression,
                        "Uri.fromFile() exposes raw file paths to other apps. " +
                            "Use FileProvider.getUriForFile() instead. [MSTG-STORAGE-2]",
                        ProblemHighlightType.WARNING
                    )
                    return
                }

                if ((receiver == "Environment" || receiver.endsWith(".Environment")) &&
                    method in setOf("getExternalStorageDirectory", "getExternalStoragePublicDirectory")
                ) {
                    holder.registerProblem(
                        expression,
                        "Environment.$method() returns a world-readable path. " +
                            "Use Context.getExternalFilesDir() for app-specific storage. [MSTG-STORAGE-2]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
