package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtVisitorVoid

class HardcodedCredentialsInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Hardcoded credentials (MSTG-STORAGE-14)"

    private val credentialKeywords = listOf("password", "apikey", "api_key", "secret", "token", "credential")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                val name = property.name?.lowercase() ?: return
                if (credentialKeywords.none { name.contains(it) }) return

                val initializer = property.initializer as? KtStringTemplateExpression ?: return
                if (initializer.entries.size != 1) return
                val entry = initializer.entries[0] as? KtLiteralStringTemplateEntry ?: return
                if (entry.text.isBlank()) return

                holder.registerProblem(
                    property,
                    "Hardcoded credential in '${property.name}'. Use BuildConfig, EncryptedSharedPreferences, or environment variables instead. [MSTG-STORAGE-14]",
                    ProblemHighlightType.WARNING
                )
            }
        }
}
