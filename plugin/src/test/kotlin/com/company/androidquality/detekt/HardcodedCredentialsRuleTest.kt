package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.HardcodedCredentialsRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HardcodedCredentialsRuleTest {
    private val rule = HardcodedCredentialsRule(Config.empty)

    @Test
    fun `detects hardcoded password property`() {
        val findings = rule.lint("""val password = "as1266375" """)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("HardcodedCredentials")
    }

    @Test
    fun `detects hardcoded apiKey property`() {
        val findings = rule.lint("""val apiKey = "sk-1234567890abcdef" """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects hardcoded token property`() {
        val findings = rule.lint("""val token = "Bearer abc123" """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `no finding for non-sensitive variable name`() {
        val findings = rule.lint("""val title = "Hello World" """)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for empty string value`() {
        val findings = rule.lint("""val password = "" """)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for variable reference (not literal)`() {
        val findings = rule.lint("""val password = System.getenv("PASSWORD") """)
        assertThat(findings).isEmpty()
    }
}
