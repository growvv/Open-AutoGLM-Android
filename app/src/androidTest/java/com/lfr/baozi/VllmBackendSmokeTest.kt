package com.lfr.baozi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lfr.baozi.network.ModelClient
import com.lfr.baozi.network.dto.ChatMessage
import com.lfr.baozi.network.dto.ContentItem
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assume
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VllmBackendSmokeTest {

    @Test
    fun vllmModelsEndpointAccessibleWithoutApiKey() {
        val baseUrl = BuildConfig.DEFAULT_BASE_URL.trim().trimEnd('/')
        Assume.assumeTrue(
            "Set PHONE_AGENT_BASE_URL (or BAOZI_DEFAULT_BASE_URL) at build time to run this test",
            baseUrl.isNotBlank() && baseUrl != "http://127.0.0.1:28100/v1"
        )

        val modelsUrl = "$baseUrl/models"
        val client = OkHttpClient.Builder().build()
        val request = Request.Builder()
            .url(modelsUrl)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }
    }

    @Test
    fun modelClientDoesNotSendAuthorizationHeaderWhenApiKeyBlank() = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "id": "test",
                          "choices": [
                            {
                              "index": 0,
                              "message": { "role": "assistant", "content": "finish(message=ok)" },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2 }
                        }
                        """.trimIndent()
                    )
            )

            val baseUrl = server.url("/v1").toString()
            val modelClient = ModelClient(
                context = context,
                baseUrl = baseUrl,
                apiKey = ""
            )

            val messages = listOf(
                ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = "test"))),
                ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = "hi")))
            )

            modelClient.request(messages = messages, modelName = "autoglm-phone-9b")
            val recorded = server.takeRequest()
            assertTrue("Authorization header should be absent", recorded.getHeader("Authorization") == null)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun modelClientAppendsV1WhenBaseUrlHasNoPath() = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "id": "test",
                          "choices": [
                            {
                              "index": 0,
                              "message": { "role": "assistant", "content": "finish(message=ok)" },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2 }
                        }
                        """.trimIndent()
                    )
            )

            val hostOnlyBaseUrl = server.url("/").toString().trimEnd('/')
            val modelClient = ModelClient(context = context, baseUrl = hostOnlyBaseUrl, apiKey = "")
            val messages =
                listOf(
                    ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = "test"))),
                    ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = "hi")))
                )

            modelClient.request(messages = messages, modelName = "autoglm-phone-9b")
            val recorded = server.takeRequest()
            assertEquals("/v1/chat/completions", recorded.path)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun modelClientSendsAuthorizationHeaderWhenApiKeyProvided() = runBlocking {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """
                        {
                          "id": "test",
                          "choices": [
                            {
                              "index": 0,
                              "message": { "role": "assistant", "content": "finish(message=ok)" },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": { "prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2 }
                        }
                        """.trimIndent()
                    )
            )

            val baseUrl = server.url("/v1").toString()
            val modelClient = ModelClient(
                context = context,
                baseUrl = baseUrl,
                apiKey = "test-key"
            )

            val messages = listOf(
                ChatMessage(role = "system", content = listOf(ContentItem(type = "text", text = "test"))),
                ChatMessage(role = "user", content = listOf(ContentItem(type = "text", text = "hi")))
            )

            modelClient.request(messages = messages, modelName = "autoglm-phone-9b")
            val recorded = server.takeRequest()
            assertEquals("Bearer test-key", recorded.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}
