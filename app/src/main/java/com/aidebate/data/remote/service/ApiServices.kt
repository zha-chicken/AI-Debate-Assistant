package com.aidebate.data.remote.service

import com.aidebate.data.remote.dto.anthropic.AnthropicMessageRequest
import com.aidebate.data.remote.dto.anthropic.AnthropicMessageResponse
import com.aidebate.data.remote.dto.gemini.GeminiGenerateRequest
import com.aidebate.data.remote.dto.gemini.GeminiGenerateResponse
import com.aidebate.data.remote.dto.openai.OpenAiChatRequest
import com.aidebate.data.remote.dto.openai.OpenAiChatResponse
import retrofit2.Response
import retrofit2.http.*

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Body request: OpenAiChatRequest
    ): Response<OpenAiChatResponse>
}

interface AnthropicApiService {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicMessageRequest
    ): Response<AnthropicMessageResponse>
}

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): Response<GeminiGenerateResponse>
}

interface OpenAiCompatibleApiService {
    @POST("v1/chat/completions")
    suspend fun chat(
        @Body request: OpenAiChatRequest
    ): Response<OpenAiChatResponse>
}
