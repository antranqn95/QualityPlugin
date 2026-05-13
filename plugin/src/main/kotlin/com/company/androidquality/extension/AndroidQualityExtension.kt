package com.company.androidquality.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

open class AndroidQualityExtension(objects: ObjectFactory) {
    val ai: AiConfig = objects.newInstance(AiConfig::class.java)
    val testGeneration: TestGenerationConfig = objects.newInstance(TestGenerationConfig::class.java)
    val security: SecurityConfig = objects.newInstance(SecurityConfig::class.java)

    fun ai(action: Action<AiConfig>) = action.execute(ai)
    fun testGeneration(action: Action<TestGenerationConfig>) = action.execute(testGeneration)
    fun security(action: Action<SecurityConfig>) = action.execute(security)
}
