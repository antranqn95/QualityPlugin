package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.*
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class WeakCryptographyRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "WeakCryptography",
        severity = Severity.Security,
        description = "Weak or broken cryptographic algorithm detected. Use SHA-256, AES/GCM, or stronger.",
        debt = Debt.TWENTY_MINS
    )

    // Algorithms that are broken or deprecated for security use
    private val weakAlgorithms = listOf(
        "md5", "sha-1", "sha1", "des", "rc4", "rc2", "blowfish"
    )

    // ECB mode is insecure for any block cipher regardless of key size
    private val insecureModes = listOf("ecb")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callText = expression.calleeExpression?.text ?: return
        // Target: MessageDigest.getInstance("...") or Cipher.getInstance("...")
        if (!callText.endsWith("getInstance")) return

        val firstArg = expression.valueArguments.firstOrNull()
            ?.getArgumentExpression() as? KtStringTemplateExpression ?: return

        val algorithmText = firstArg.entries
            .filterIsInstance<KtLiteralStringTemplateEntry>()
            .joinToString("") { it.text }
            .lowercase()

        val isWeak = weakAlgorithms.any { algorithmText == it || algorithmText.startsWith("$it/") } ||
            insecureModes.any { "/ecb/" in algorithmText }

        if (isWeak) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "Weak algorithm '$algorithmText' detected. Use SHA-256 for hashing, AES/GCM/NoPadding for encryption."
                )
            )
        }
    }
}
