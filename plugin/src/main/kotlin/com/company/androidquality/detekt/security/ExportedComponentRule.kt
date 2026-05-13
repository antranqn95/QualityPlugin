package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtClass

class ExportedComponentRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ExportedComponent",
        severity = Severity.Security,
        description = "Android component without permission protection may be accessible by other apps.",
        debt = Debt.TWENTY_MINS
    )

    private val exportedComponents = setOf(
        "BroadcastReceiver", "ContentProvider", "Service"
    )

    private val permissionChecks = setOf(
        "checkCallingPermission", "checkCallingOrSelfPermission",
        "enforceCallingPermission", "enforceCallingOrSelfPermission",
        "checkPermission", "enforcePermission"
    )

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)

        val superTypeNames = klass.superTypeListEntries.map { it.text.substringBefore("(").trim() }
        val matchedComponent = exportedComponents.firstOrNull { superTypeNames.contains(it) } ?: return

        val classBody = klass.body?.text ?: return
        val hasPermissionCheck = permissionChecks.any { classBody.contains(it) }

        if (!hasPermissionCheck) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(klass),
                    "${klass.name} extends $matchedComponent without any permission check. " +
                        "Ensure it is protected in the manifest (android:permission) or add runtime permission checks (MSTG-PLATFORM-1)."
                )
            )
        }
    }
}
