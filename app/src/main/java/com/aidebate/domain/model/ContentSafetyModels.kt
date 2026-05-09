package com.aidebate.domain.model

enum class ContentSafetySource {
    USER_PROMPT,
    AI_RESPONSE
}

data class ContentSafetyResult(
    val isAllowed: Boolean,
    val reasons: List<String> = emptyList(),
    val serviceFailed: Boolean = false
)

class ContentSafetyException(
    val result: ContentSafetyResult,
    source: ContentSafetySource
) : Exception(
    buildString {
        if (result.serviceFailed) {
            append("\u5185\u5bb9\u5b89\u5168\u68c0\u6d4b\u670d\u52a1\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5")
        } else {
            append(if (source == ContentSafetySource.USER_PROMPT) "Your input" else "The AI response")
            append(" was blocked by content safety")
            if (result.reasons.isNotEmpty()) {
                append(": ")
                append(result.reasons.joinToString(", "))
            }
            append(".")
        }
    }
)
