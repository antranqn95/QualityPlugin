package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.LogSensitiveDataRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LogSensitiveDataRuleTest {
    private val rule = LogSensitiveDataRule(Config.empty)

    @Test
    fun `detects Log_d with password variable in message`() {
        val findings = rule.lint("""
            fun test() { Log.d(TAG, "user password: ${"$"}password") }
        """)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("LogSensitiveData")
    }

    @Test
    fun `detects Log_e with token in message`() {
        val findings = rule.lint("""
            fun test() { Log.e(TAG, "auth token=${"$"}token") }
        """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects Log_i with secret keyword in string literal`() {
        val findings = rule.lint("""
            fun test() { Log.i("App", "secret key loaded") }
        """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects Log_w with apiKey reference`() {
        val findings = rule.lint("""
            fun test() { Log.w(TAG, "apiKey=${"$"}apiKey") }
        """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects println with password`() {
        val findings = rule.lint("""
            fun test() { println("password=${"$"}userPassword") }
        """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `no finding for Log_d with safe message`() {
        val findings = rule.lint("""
            fun test() { Log.d(TAG, "User logged in successfully") }
        """)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for Log_d with only tag containing sensitive word`() {
        // Only the MESSAGE argument should be checked, not the tag
        val findings = rule.lint("""
            fun test() { Log.d("PasswordManager", "Operation complete") }
        """)
        assertThat(findings).isEmpty()
    }
}
