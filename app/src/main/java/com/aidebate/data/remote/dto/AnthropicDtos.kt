package com.aidebate.data.remote.dto.anthropic

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnthropicMessageRequest(
    val model: String,
    @Json(name = "max_tokens") val maxTokens: Int = 1024,
    val system: String? = null,
    val messages: List<AnthropicMessage>,
    val temperature: Double? = 0.7
)

@JsonClass(generateAdapter = true)
data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContent>
)

@JsonClass(generateAdapter = true)
data class AnthropicContent(
    val type: String = "text",
    val text: String
)

@JsonClass(generateAdapter = true)
data class AnthropicMessageResponse(
    val id: String,
    val content: List<AnthropicContent>,
    val role: String,
    val usage: AnthropicUsage?
)

@JsonClass(generateAdapter = true)
data class AnthropicUsage(
    @Json(name = "input_tokens") val inputTokens: Int,
    @Json(name = "output_tokens") val outputTokens: Int
)
