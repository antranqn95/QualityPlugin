package com.company.androidquality.ai

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class AITestGenerator(
    private val apiKey: String,
    private val model: String,
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build(),
) {

    fun generate(classInfo: ClassInfo, testFramework: String): String {
        val requestBody = buildRequestBody(classInfo, testFramework)
        val response = sendRequest(requestBody)

        if (response.statusCode() == 429) {
            Thread.sleep(2000)
            val retry = sendRequest(requestBody)
            return parseContent(retry.body())
        }
        if (response.statusCode() != 200) {
            throw RuntimeException("Anthropic API error ${response.statusCode()}: ${response.body()}")
        }
        return parseContent(response.body())
    }

    private fun sendRequest(body: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(60))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun buildRequestBody(classInfo: ClassInfo, testFramework: String): String {
        val prompt = buildPrompt(classInfo, testFramework)
        val body = mapOf(
            "model" to model,
            "max_tokens" to 4096,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt))
        )
        return groovy.json.JsonOutput.toJson(body)
    }

    private fun buildPrompt(classInfo: ClassInfo, testFramework: String): String {
        val flowRequirements = if (classInfo.hasFlowTypes) """
        - This class uses StateFlow/SharedFlow/Flow — test all flows with the Turbine library:
          * Wrap flow assertions: runTest { flow.test { assertEquals(expected, awaitItem()) } }
          * StateFlow: first awaitItem() returns the current/initial value
          * SharedFlow/event flows: use expectNoEvents() before triggering the action, then awaitItem()
          * Always end with cancelAndIgnoreRemainingEvents() or awaitComplete()
          * Add to test dependencies: testImplementation("app.cash.turbine:turbine:1.1.0")
        """.trimIndent() else ""

        return """
        Generate comprehensive JUnit unit tests for the following Kotlin class.

        Requirements:
        - Use $testFramework annotations (@Test, @Before, @After)
        - Use MockK for mocking: mockk(), coEvery {}, coVerify {}, verify {}
        - Use kotlinx-coroutines-test with runTest {} for all suspend functions and Flow tests
        ${if (flowRequirements.isNotEmpty()) flowRequirements else ""}
        - Test happy path AND failure/error cases for each public method
        - Use descriptive backtick test names: fun `method returns X when Y`()
        - Include the package declaration: package ${classInfo.packageName}
        - Return ONLY the complete Kotlin test file content, no markdown, no explanation

        Class to test:
        ```kotlin
        ${classInfo.source}
        ```
        """.trimIndent()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseContent(responseBody: String): String {
        val json = groovy.json.JsonSlurper().parseText(responseBody) as Map<String, Any>
        val content = (json["content"] as List<Map<String, Any>>).first()
        val text = content["text"] as String
        return stripMarkdownFences(text)
    }

    private fun stripMarkdownFences(text: String): String {
        val trimmed = text.trim()
        val fencePattern = Regex("""^```(?:kotlin)?\r?\n(.*?)\r?\n```\s*$""", RegexOption.DOT_MATCHES_ALL)
        return fencePattern.find(trimmed)?.groupValues?.get(1) ?: trimmed
    }
}
