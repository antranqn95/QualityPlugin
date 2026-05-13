package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.InsecureHttpUsageRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InsecureHttpUsageRuleTest {
    private val rule = InsecureHttpUsageRule(Config.empty)

    @Test
    fun `detects plain http URL string`() {
        val findings = rule.lint("""val url = "http://api.example.com/v1" """)
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureHttpUsage")
    }

    @Test
    fun `detects http URL with port`() {
        val findings = rule.lint("""val host = "http://7go.xyz:8080" """)
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `no finding for https URL`() {
        val findings = rule.lint("""val url = "https://api.example.com/v1" """)
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for non-URL string`() {
        val findings = rule.lint("""val label = "http is a protocol" """)
        assertThat(findings).isEmpty()
    }
}
