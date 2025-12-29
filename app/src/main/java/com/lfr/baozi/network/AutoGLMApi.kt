package com.lfr.baozi.network

import com.lfr.baozi.network.dto.ChatRequest
import com.lfr.baozi.network.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AutoGLMApi {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
