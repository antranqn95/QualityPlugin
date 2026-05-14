package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.UnencryptedSensitiveStorageRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnencryptedSensitiveStorageRuleTest {
    private val rule = UnencryptedSensitiveStorageRule(Config.empty)

    @Test
    fun `detects putString with password key`() {
        val findings = rule.lint("""
            fun test() {
                val prefs = getSharedPreferences("test", MODE_PRIVATE)
                prefs.edit().putString("password_key", value).apply()
            }
        """)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("UnencryptedSensitiveStorage")
    }

    @Test
    fun `detects getString with token key`() {
        val findings = rule.lint("""
            val prefs = getSharedPreferences("test", MODE_PRIVATE)
            val t = prefs.getString("auth_token", null)
        """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `no finding for non-sensitive key`() {
        val findings = rule.lint("""
            val prefs = getSharedPreferences("test", MODE_PRIVATE)
            prefs.edit().putString("username_display", value).apply()
        """)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for non-prefs call with sensitive name`() {
        val findings = rule.lint("""log("token received")""")
        assertThat(findings).isEmpty()
    }
}
