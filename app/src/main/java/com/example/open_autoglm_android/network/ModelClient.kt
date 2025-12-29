package com.example.open_autoglm_android.network

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.open_autoglm_android.R
import com.example.open_autoglm_android.network.dto.*
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ModelResponse(
    val thinking: String,
    val action: String
)

class ModelClient(
    private val context: Context,
    baseUrl: String,
    private val apiKey: String
) {
    private val api: AutoGLMApi
    
    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Authorization", if (apiKey.isBlank() || apiKey == "EMPTY") "Bearer EMPTY" else "Bearer $apiKey")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
        
        // 验证并修复URL格式
        val validatedBaseUrl = validateAndFixUrl(baseUrl)
        
        val retrofit = Retrofit.Builder()
            .baseUrl(validatedBaseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(AutoGLMApi::class.java)
    }
    
    /**
     * 验证并修复URL格式
     */
    private fun validateAndFixUrl(url: String): String {
        var fixedUrl = url.trim()
        // 如果URL没有协议头，添加默认的https协议
        if (!fixedUrl.startsWith("http://") && !fixedUrl.startsWith("https://")) {
            fixedUrl = "https://$fixedUrl"
        }
        return fixedUrl
    }
    
    /**
     * 请求模型（使用消息上下文）
     */
    suspend fun request(
        messages: List<ChatMessage>,
        modelName: String
    ): ModelResponse {
        val request = ChatRequest(
            model = modelName,
            messages = messages,
            maxTokens = 3000,
            temperature = 0.0,
            topP = 0.85,
            frequencyPenalty = 0.2,
            stream = false
        )
        
        val response = api.chatCompletion(request)
        
        if (response.isSuccessful && response.body() != null) {
            val responseBody = response.body()!!
            val content = responseBody.choices.firstOrNull()?.message?.content ?: ""
            return parseResponse(content)
        } else {
            throw Exception("API request failed: ${response.code()} ${response.message()}")
        }
    }
    
    /**
     * 创建系统消息
     */
    fun createSystemMessage(): ChatMessage {
        val systemPrompt = buildSystemPrompt()
        return ChatMessage(
            role = "system",
            content = listOf(ContentItem(type = "text", text = systemPrompt))
        )
    }
    
    /**
     * 创建消息的通用基础方法
     */
    private fun createMessage(
        text: String,
        screenshot: Bitmap?,
        currentApp: String?,
        quality: Int = 80
    ): ChatMessage {
        val userContent = mutableListOf<ContentItem>()
        val screenInfoJson = buildScreenInfo(currentApp)
        val fullText = if (text.isEmpty()) screenInfoJson else "$text\n\n$screenInfoJson"

        // 对齐旧项目：先放图片，再放文本
        screenshot?.let { bitmap ->
            val base64Image = bitmapToBase64(bitmap, quality)
            userContent.add(
                ContentItem(
                    type = "image_url",
                    imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64Image")
                )
            )
        }

        userContent.add(ContentItem(type = "text", text = fullText))
        return ChatMessage(role = "user", content = userContent)
    }

    /**
     * 创建用户消息（第一次调用，包含原始任务）
     */
    fun createUserMessage(userPrompt: String, screenshot: Bitmap?, currentApp: String?, quality: Int = 80): ChatMessage {
        return createMessage(userPrompt, screenshot, currentApp, quality)
    }
    
    /**
     * 创建屏幕信息消息（后续调用，只包含屏幕信息）
     */
    fun createScreenInfoMessage(screenshot: Bitmap?, currentApp: String?, quality: Int = 80): ChatMessage {
        return createMessage("** Screen Info **", screenshot, currentApp, quality)
    }
    
    /**
     * 创建助手消息（添加到上下文）
     */
    fun createAssistantMessage(thinking: String, action: String): ChatMessage {
        val content = "<think>$thinking</think><answer>$action</answer>"
        return ChatMessage(
            role = "assistant",
            content = listOf(ContentItem(type = "text", text = content))
        )
    }
    
    /**
     * 构建屏幕信息（使用 JsonObject 确保转义安全）
     */
    private fun buildScreenInfo(currentApp: String?): String {
        val json = JsonObject()
        json.addProperty("current_app", currentApp ?: "Unknown")
        return json.toString()
    }
    
    /**
     * 从消息中移除图片内容，只保留文本（节省 token）
     */
    fun removeImagesFromMessage(message: ChatMessage): ChatMessage {
        val textOnlyContent = message.content.filter { it.type == "text" }
        return ChatMessage(
            role = message.role,
            content = textOnlyContent
        )
    }
    
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
        return ByteArrayOutputStream().use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }
    }
    
    private fun buildSystemPrompt(): String {
        val template = context.getString(R.string.system_prompt_template)
        return String.format(template, java.time.LocalDate.now())
    }
    
    private fun parseResponse(content: String): ModelResponse {
        Log.d("ModelClient", "解析响应内容: ${content.take(500)}")
        
        var thinking = ""
        var action = ""
        
        if (content.contains("finish(message=")) {
            val parts = content.split("finish(message=", limit = 2)
            thinking = parts[0].trim()
            action = "finish(message=" + parts[1]
        } else if (content.contains("do(action=")) {
            val parts = content.split("do(action=", limit = 2)
            thinking = parts[0].trim()
            action = "do(action=" + parts[1]
        } else if (content.contains("<answer>")) {
            val parts = content.split("<answer>", limit = 2)
            thinking = parts[0]
                .replace("<think>", "")
                .replace("</think>", "")
                .replace("<redacted_reasoning>", "")
                .replace("</redacted_reasoning>", "")
                .trim()
            action = parts[1].replace("</answer>", "").trim()
        } else {
            action = content.trim()
        }
        
        if (!action.startsWith("{") && !action.startsWith("do(") && !action.startsWith("finish(")) {
            val funcMatch = Regex("""(do|finish)\s*\([^)]+\)""", RegexOption.IGNORE_CASE).find(content)
            if (funcMatch != null) {
                action = funcMatch.value
            } else {
                val extractedJson = extractJsonFromContent(content)
                if (extractedJson.isNotEmpty()) {
                    action = extractedJson
                }
            }
        }
        
        return ModelResponse(thinking = thinking, action = action)
    }
    
    private fun extractJsonFromContent(content: String): String {
        var startIndex = -1
        var braceCount = 0
        val candidates = mutableListOf<String>()
        
        for (i in content.indices) {
            when (content[i]) {
                '{' -> {
                    if (startIndex == -1) startIndex = i
                    braceCount++
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0 && startIndex != -1) {
                        val candidate = content.substring(startIndex, i + 1)
                        try {
                            com.google.gson.JsonParser.parseString(candidate)
                            candidates.add(candidate)
                        } catch (e: Exception) {}
                        startIndex = -1
                    }
                }
            }
        }
        return candidates.firstOrNull() ?: ""
    }
    
    private fun String.ensureTrailingSlash(): String {
        return if (this.endsWith("/")) this else "$this/"
    }
}
