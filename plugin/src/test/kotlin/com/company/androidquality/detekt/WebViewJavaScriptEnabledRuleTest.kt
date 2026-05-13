package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.WebViewJavaScriptEnabledRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WebViewJavaScriptEnabledRuleTest {
    private val rule = WebViewJavaScriptEnabledRule(Config.empty)

    @Test
    fun `detects webView settings javaScriptEnabled set to true`() {
        val findings = rule.lint("""
            fun test() {
                webView.settings.javaScriptEnabled = true
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("WebViewJavaScriptEnabled")
    }

    @Test
    fun `detects settings javaScriptEnabled set to true`() {
        val findings = rule.lint("""
            fun test() {
                settings.javaScriptEnabled = true
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("WebViewJavaScriptEnabled")
    }

    @Test
    fun `no finding when javaScriptEnabled set to false`() {
        val findings = rule.lint("""
            fun test() {
                webView.settings.javaScriptEnabled = false
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding when javaScriptEnabled set to non-literal variable`() {
        val findings = rule.lint("""
            fun test() {
                webView.settings.javaScriptEnabled = isDebug
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
