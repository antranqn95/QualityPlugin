package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.DeepLinkInjectionRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeepLinkInjectionRuleTest {
    private val rule = DeepLinkInjectionRule(Config.empty)

    @Test
    fun `detects getQueryParameter passed directly to loadUrl`() {
        val findings = rule.lint("""
            fun handleDeepLink(uri: Uri) {
                val url = uri.getQueryParameter("redirect")
                webView.loadUrl(url)
            }
        """.trimIndent())
        assertThat(findings).isNotEmpty
        assertThat(findings[0].id).isEqualTo("DeepLinkInjection")
    }

    @Test
    fun `detects getStringExtra passed directly to loadUrl`() {
        val findings = rule.lint("""
            fun handleIntent(intent: Intent) {
                val url = intent.getStringExtra("url")
                webView.loadUrl(url)
            }
        """.trimIndent())
        assertThat(findings).isNotEmpty
        assertThat(findings[0].id).isEqualTo("DeepLinkInjection")
    }

    @Test
    fun `no finding when loadUrl is called with hardcoded string`() {
        val findings = rule.lint("""
            fun openPage() {
                webView.loadUrl("https://example.com/safe-page")
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding when external input is used in non-sensitive context`() {
        val findings = rule.lint("""
            fun handleIntent(intent: Intent) {
                val title = intent.getStringExtra("title")
                textView.text = title
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
