package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.InsecureRandomRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InsecureRandomRuleTest {
    private val rule = InsecureRandomRule(Config.empty)

    @Test
    fun `detects Random() instantiation`() {
        val findings = rule.lint("""
            fun test() {
                val r = Random()
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureRandom")
    }

    @Test
    fun `detects java util Random() instantiation`() {
        val findings = rule.lint("""
            fun test() {
                val r = java.util.Random()
            }
        """.trimIndent())
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("InsecureRandom")
    }

    @Test
    fun `no finding for SecureRandom()`() {
        val findings = rule.lint("""
            fun test() {
                val r = SecureRandom()
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for java security SecureRandom()`() {
        val findings = rule.lint("""
            fun test() {
                val r = java.security.SecureRandom()
            }
        """.trimIndent())
        assertThat(findings).isEmpty()
    }
}
