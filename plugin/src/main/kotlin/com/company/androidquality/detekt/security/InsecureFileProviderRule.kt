package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

class InsecureFileProviderRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "InsecureFileProvider",
        severity = Severity.Security,
        description = "Direct file URI exposure or external storage access without FileProvider may leak sensitive data.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val receiverText = expression.receiverExpression.text
        val selectorCall = expression.selectorExpression as? KtCallExpression ?: return
        val methodName = selectorCall.calleeExpression?.text ?: return

        // Uri.fromFile() — should use FileProvider.getUriForFile() instead
        if ((receiverText == "Uri" || receiverText.endsWith(".Uri")) && methodName == "fromFile") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Uri.fromFile() exposes raw file paths to other apps. " +
                        "Use FileProvider.getUriForFile() instead (MSTG-STORAGE-2)."
                )
            )
        }

        // Environment.getExternalStorageDirectory() — data stored here is world-readable
        if ((receiverText == "Environment" || receiverText.endsWith(".Environment")) &&
            methodName == "getExternalStorageDirectory"
        ) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Environment.getExternalStorageDirectory() returns a world-readable path. " +
                        "Use app-specific external storage (Context.getExternalFilesDir()) instead (MSTG-STORAGE-2)."
                )
            )
        }

        // Environment.getExternalStoragePublicDirectory() — same issue
        if ((receiverText == "Environment" || receiverText.endsWith(".Environment")) &&
            methodName == "getExternalStoragePublicDirectory"
        ) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Environment.getExternalStoragePublicDirectory() returns a world-readable path. " +
                        "Store sensitive data in app-specific directories instead (MSTG-STORAGE-2)."
                )
            )
        }
    }
}
