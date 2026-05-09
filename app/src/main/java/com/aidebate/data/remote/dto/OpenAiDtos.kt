package com.aidebate.data.remote.dto.openai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double? = 0.7,
    @Json(name = "max_tokens") val maxTokens: Int? = 1024,
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@JsonClass(generateAdapter = true)
data class Choice(
    val index: Int,
    val message: OpenAiMessage,
    @Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int,
    @Json(name = "completion_tokens") val completionTokens: Int,
    @Json(name = "total_tokens") val totalTokens: Int
)

@JsonClass(generateAdapter = true)
data class OpenAiModerationRequest(
    val model: String = "omni-moderation-latest",
    val input: String
)

@JsonClass(generateAdapter = true)
data class OpenAiModerationResponse(
    val id: String? = null,
    val model: String? = null,
    val results: List<OpenAiModerationResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenAiModerationResult(
    val flagged: Boolean = false,
    val categories: Map<String, Boolean> = emptyMap(),
    @Json(name = "category_scores") val categoryScores: Map<String, Double> = emptyMap()
)
