package com.example.open_autoglm_android.network.dto

import com.google.gson.annotations.SerializedName

data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ResponseMessage(
    val role: String,
    val content: String
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
