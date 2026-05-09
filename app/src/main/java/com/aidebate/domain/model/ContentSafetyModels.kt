package com.aidebate.domain.model

enum class ContentSafetySource {
    USER_PROMPT,
    AI_RESPONSE
}

data class ContentSafetyResult(
    val isAllowed: Boolean,
    val reasons: List<String> = emptyList()
)

class ContentSafetyException(
    val result: ContentSafetyResult,
    source: ContentSafetySource
) : Exception(
    buildString {
        append(if (source == ContentSafetySource.USER_PROMPT) "Your input" else "The AI response")
        append(" was blocked by content safety")
        if (result.reasons.isNotEmpty()) {
            append(": ")
            append(result.reasons.joinToString(", "))
        }
        append(".")
    }
)
