package com.company.androidquality.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtVisitorVoid

class ExportedComponentInspection : LocalInspectionTool() {

    override fun getGroupDisplayName() = "Security (OWASP MASVS)"
    override fun getDisplayName() = "Exported component without permission check (MSTG-PLATFORM-1)"

    private val exportedComponents = setOf("BroadcastReceiver", "ContentProvider", "Service")
    private val permissionChecks = setOf(
        "checkCallingPermission", "checkCallingOrSelfPermission",
        "enforceCallingPermission", "enforceCallingOrSelfPermission",
        "checkPermission", "enforcePermission"
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : KtVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                val superTypeNames = klass.superTypeListEntries
                    .map { it.text.substringBefore("(").trim() }
                val component = exportedComponents.firstOrNull { superTypeNames.contains(it) } ?: return

                val classBody = klass.body?.text ?: return
                if (permissionChecks.none { classBody.contains(it) }) {
                    holder.registerProblem(
                        klass.nameIdentifier ?: klass,
                        "${klass.name} extends $component without any runtime permission check. " +
                            "Add android:permission in manifest or call checkCallingPermission() at runtime. [MSTG-PLATFORM-1]",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
}
