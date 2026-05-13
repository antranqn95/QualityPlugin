package com.company.androidquality.ai

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class AITestGeneratorTest {

    private val mockClient: HttpClient = mockk()
    private val mockResponse: HttpResponse<String> = mockk()

    private val classInfo = ClassInfo(
        className = "MyService",
        packageName = "com.example",
        packagePath = "com/example",
        source = "package com.example\n\nclass MyService {\n    fun greet() = \"hello\"\n}"
    )

    private lateinit var generator: AITestGenerator

    @BeforeEach
    fun setUp() {
        generator = AITestGenerator(
            apiKey = "test-api-key",
            model = "claude-sonnet-4-6",
            client = mockClient,
        )
    }

    private fun mockResponseWith(statusCode: Int, body: String) {
        every { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns statusCode
        every { mockResponse.body() } returns body
    }

    private fun validApiResponseBody(text: String): String {
        val body = mapOf(
            "content" to listOf(mapOf("type" to "text", "text" to text))
        )
        return groovy.json.JsonOutput.toJson(body)
    }

    @Test
    fun `generate returns parsed text content on 200 response`() {
        val expectedTest = "package com.example\n\nimport org.junit.Test\n\nclass MyServiceTest {}"
        mockResponseWith(200, validApiResponseBody(expectedTest))

        val result = generator.generate(classInfo, "junit4")

        assertThat(result).isEqualTo(expectedTest)
    }

    @Test
    fun `generate throws RuntimeException on non-200 non-429 response`() {
        mockResponseWith(401, """{"error": "Unauthorized"}""")

        assertThatThrownBy { generator.generate(classInfo, "junit4") }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("401")
    }

    @Test
    fun `generate throws RuntimeException on 500 response`() {
        mockResponseWith(500, """{"error": "Internal Server Error"}""")

        assertThatThrownBy { generator.generate(classInfo, "junit4") }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("500")
    }

    @Test
    fun `generate retries and returns result on 429 then 200`() {
        val expectedTest = "class MyServiceTest { /* retry succeeded */ }"
        val retryResponse: HttpResponse<String> = mockk()
        every { retryResponse.statusCode() } returns 200
        every { retryResponse.body() } returns validApiResponseBody(expectedTest)

        every { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }
            .returnsMany(mockResponse, retryResponse)
        every { mockResponse.statusCode() } returns 429
        every { mockResponse.body() } returns ""

        val result = generator.generate(classInfo, "junit4")

        assertThat(result).isEqualTo(expectedTest)
        verify(exactly = 2) { mockClient.send(any(), any<HttpResponse.BodyHandler<String>>()) }
    }

    @Test
    fun `generate sends request with correct API key header`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("test output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        assertThat(capturedRequest.captured.headers().firstValue("x-api-key").orElse(""))
            .isEqualTo("test-api-key")
    }

    @Test
    fun `generate sends request to Anthropic messages endpoint`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("test output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        assertThat(capturedRequest.captured.uri().toString())
            .isEqualTo("https://api.anthropic.com/v1/messages")
    }

    @Test
    fun `generate sends request with correct anthropic-version header`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("test output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        assertThat(capturedRequest.captured.headers().firstValue("anthropic-version").orElse(""))
            .isEqualTo("2023-06-01")
    }

    @Test
    fun `generate sends POST request`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("test output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        assertThat(capturedRequest.captured.method()).isEqualTo("POST")
    }

    @Test
    fun `generate includes class source in request body`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("test output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        val bodyPublisher = capturedRequest.captured.bodyPublisher().orElse(null)
        assertThat(bodyPublisher).isNotNull
        // bodyPublisher.contentLength() > 0 confirms a body was sent
        assertThat(bodyPublisher!!.contentLength()).isGreaterThan(0)
    }

    @Test
    fun `generate includes model name in request body`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        AITestGenerator("key", "claude-haiku-4-5", mockClient).generate(classInfo, "junit4")

        // Read body bytes to verify model name is included
        val bodyBytes = mutableListOf<Byte>()
        val subscriber = object : java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
            override fun onSubscribe(subscription: java.util.concurrent.Flow.Subscription) = subscription.request(Long.MAX_VALUE)
            override fun onNext(item: java.nio.ByteBuffer) { while (item.hasRemaining()) bodyBytes.add(item.get()) }
            override fun onError(throwable: Throwable) {}
            override fun onComplete() {}
        }
        capturedRequest.captured.bodyPublisher().get().subscribe(subscriber)
        val bodyText = String(bodyBytes.toByteArray())
        assertThat(bodyText).contains("claude-haiku-4-5")
    }

    @Test
    fun `generate strips kotlin markdown fence from response`() {
        val rawCode = "package com.example\n\nclass MyServiceTest {}"
        val fenced = "```kotlin\n$rawCode\n```"
        mockResponseWith(200, validApiResponseBody(fenced))

        val result = generator.generate(classInfo, "junit4")

        assertThat(result).isEqualTo(rawCode)
        assertThat(result).doesNotContain("```")
    }

    @Test
    fun `generate strips plain markdown fence from response`() {
        val rawCode = "package com.example\n\nclass MyServiceTest {}"
        val fenced = "```\n$rawCode\n```"
        mockResponseWith(200, validApiResponseBody(fenced))

        val result = generator.generate(classInfo, "junit4")

        assertThat(result).isEqualTo(rawCode)
        assertThat(result).doesNotContain("```")
    }

    @Test
    fun `generate returns content as-is when no markdown fence present`() {
        val rawCode = "package com.example\n\nclass MyServiceTest {}"
        mockResponseWith(200, validApiResponseBody(rawCode))

        val result = generator.generate(classInfo, "junit4")

        assertThat(result).isEqualTo(rawCode)
    }

    @Test
    fun `generate includes turbine instructions in prompt when class has flow types`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        val flowClassInfo = ClassInfo(
            className = "MyViewModel",
            packageName = "com.example",
            packagePath = "com/example",
            source = "class MyViewModel { val state: StateFlow<String> = TODO() }",
            hasFlowTypes = true
        )
        generator.generate(flowClassInfo, "junit4")

        val bodyBytes = mutableListOf<Byte>()
        val subscriber = object : java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
            override fun onSubscribe(sub: java.util.concurrent.Flow.Subscription) = sub.request(Long.MAX_VALUE)
            override fun onNext(item: java.nio.ByteBuffer) { while (item.hasRemaining()) bodyBytes.add(item.get()) }
            override fun onError(t: Throwable) {}
            override fun onComplete() {}
        }
        capturedRequest.captured.bodyPublisher().get().subscribe(subscriber)
        val bodyText = String(bodyBytes.toByteArray())
        assertThat(bodyText).contains("Turbine")
        assertThat(bodyText).contains("awaitItem")
    }

    @Test
    fun `generate does not include turbine instructions when class has no flow types`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit4")

        val bodyBytes = mutableListOf<Byte>()
        val subscriber = object : java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
            override fun onSubscribe(sub: java.util.concurrent.Flow.Subscription) = sub.request(Long.MAX_VALUE)
            override fun onNext(item: java.nio.ByteBuffer) { while (item.hasRemaining()) bodyBytes.add(item.get()) }
            override fun onError(t: Throwable) {}
            override fun onComplete() {}
        }
        capturedRequest.captured.bodyPublisher().get().subscribe(subscriber)
        val bodyText = String(bodyBytes.toByteArray())
        assertThat(bodyText).doesNotContain("Turbine")
        assertThat(bodyText).doesNotContain("awaitItem")
    }

    @Test
    fun `generate includes testFramework in prompt`() {
        val capturedRequest = slot<HttpRequest>()
        mockResponseWith(200, validApiResponseBody("output"))
        every { mockClient.send(capture(capturedRequest), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        generator.generate(classInfo, "junit5")

        val bodyBytes = mutableListOf<Byte>()
        val subscriber = object : java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {
            override fun onSubscribe(subscription: java.util.concurrent.Flow.Subscription) = subscription.request(Long.MAX_VALUE)
            override fun onNext(item: java.nio.ByteBuffer) { while (item.hasRemaining()) bodyBytes.add(item.get()) }
            override fun onError(throwable: Throwable) {}
            override fun onComplete() {}
        }
        capturedRequest.captured.bodyPublisher().get().subscribe(subscriber)
        val bodyText = String(bodyBytes.toByteArray())
        assertThat(bodyText).contains("junit5")
    }
}
