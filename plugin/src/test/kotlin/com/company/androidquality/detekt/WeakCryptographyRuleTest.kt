package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.WeakCryptographyRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WeakCryptographyRuleTest {
    private val rule = WeakCryptographyRule(Config.empty)

    @Test
    fun `detects MD5 MessageDigest`() {
        val findings = rule.lint("""val md = MessageDigest.getInstance("MD5")""")
        assertThat(findings).hasSize(1)
        assertThat(findings[0].id).isEqualTo("WeakCryptography")
    }

    @Test
    fun `detects SHA-1 MessageDigest`() {
        val findings = rule.lint("""val sha = MessageDigest.getInstance("SHA-1")""")
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects SHA1 MessageDigest (no dash)`() {
        val findings = rule.lint("""val sha = MessageDigest.getInstance("SHA1")""")
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects AES ECB mode Cipher`() {
        val findings = rule.lint("""val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")""")
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects DES Cipher`() {
        val findings = rule.lint("""val cipher = Cipher.getInstance("DES")""")
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `detects RC4 Cipher`() {
        val findings = rule.lint("""val cipher = Cipher.getInstance("RC4")""")
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `no finding for SHA-256`() {
        val findings = rule.lint("""val sha = MessageDigest.getInstance("SHA-256")""")
        assertThat(findings).isEmpty()
    }

    @Test
    fun `no finding for AES GCM mode`() {
        val findings = rule.lint("""val cipher = Cipher.getInstance("AES/GCM/NoPadding")""")
        assertThat(findings).isEmpty()
    }
}
