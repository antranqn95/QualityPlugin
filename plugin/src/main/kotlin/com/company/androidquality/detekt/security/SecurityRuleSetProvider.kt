package com.company.androidquality.detekt.security

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class SecurityRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "android-security"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            HardcodedCredentialsRule(config.subConfig("HardcodedCredentials")),
            InsecureHttpUsageRule(config.subConfig("InsecureHttpUsage")),
            UnencryptedSensitiveStorageRule(config.subConfig("UnencryptedSensitiveStorage")),
            WeakCryptographyRule(config.subConfig("WeakCryptography")),
            LogSensitiveDataRule(config.subConfig("LogSensitiveData")),
            WebViewJavaScriptEnabledRule(config.subConfig("WebViewJavaScriptEnabled")),
            InsecureRandomRule(config.subConfig("InsecureRandom")),
            ExportedComponentRule(config.subConfig("ExportedComponent")),
            DeepLinkInjectionRule(config.subConfig("DeepLinkInjection")),
            InsecureFileProviderRule(config.subConfig("InsecureFileProvider"))
        )
    )
}
