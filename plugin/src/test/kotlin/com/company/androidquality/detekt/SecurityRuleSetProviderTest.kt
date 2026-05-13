package com.company.androidquality.detekt

import com.company.androidquality.detekt.security.DeepLinkInjectionRule
import com.company.androidquality.detekt.security.ExportedComponentRule
import com.company.androidquality.detekt.security.HardcodedCredentialsRule
import com.company.androidquality.detekt.security.InsecureFileProviderRule
import com.company.androidquality.detekt.security.InsecureHttpUsageRule
import com.company.androidquality.detekt.security.InsecureRandomRule
import com.company.androidquality.detekt.security.LogSensitiveDataRule
import com.company.androidquality.detekt.security.SecurityRuleSetProvider
import com.company.androidquality.detekt.security.UnencryptedSensitiveStorageRule
import com.company.androidquality.detekt.security.WeakCryptographyRule
import com.company.androidquality.detekt.security.WebViewJavaScriptEnabledRule
import io.gitlab.arturbosch.detekt.api.Config
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SecurityRuleSetProviderTest {

    private val provider = SecurityRuleSetProvider()

    @Test
    fun `ruleSetId is android-security`() {
        assertThat(provider.ruleSetId).isEqualTo("android-security")
    }

    @Test
    fun `instance returns rule set with correct id`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.id).isEqualTo("android-security")
    }

    @Test
    fun `instance returns exactly 10 rules`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).hasSize(10)
    }

    @Test
    fun `instance includes HardcodedCredentialsRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is HardcodedCredentialsRule }
    }

    @Test
    fun `instance includes InsecureHttpUsageRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is InsecureHttpUsageRule }
    }

    @Test
    fun `instance includes UnencryptedSensitiveStorageRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is UnencryptedSensitiveStorageRule }
    }

    @Test
    fun `instance includes WeakCryptographyRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is WeakCryptographyRule }
    }

    @Test
    fun `instance includes LogSensitiveDataRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is LogSensitiveDataRule }
    }

    @Test
    fun `instance includes WebViewJavaScriptEnabledRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is WebViewJavaScriptEnabledRule }
    }

    @Test
    fun `instance includes InsecureRandomRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is InsecureRandomRule }
    }

    @Test
    fun `instance includes ExportedComponentRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is ExportedComponentRule }
    }

    @Test
    fun `instance includes DeepLinkInjectionRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is DeepLinkInjectionRule }
    }

    @Test
    fun `instance includes InsecureFileProviderRule`() {
        val ruleSet = provider.instance(Config.empty)
        assertThat(ruleSet.rules).anyMatch { it is InsecureFileProviderRule }
    }

    @Test
    fun `instance creates new rule set on each call`() {
        val ruleSet1 = provider.instance(Config.empty)
        val ruleSet2 = provider.instance(Config.empty)
        assertThat(ruleSet1).isNotSameAs(ruleSet2)
    }
}
