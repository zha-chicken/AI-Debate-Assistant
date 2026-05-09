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
        append("内容安全拦截")
        append(if (source == ContentSafetySource.USER_PROMPT) "：你的输入" else "：AI 回答")
        append("包含不允许的内容")
        if (result.reasons.isNotEmpty()) {
            append("（")
            append(result.reasons.joinToString("、"))
            append("）")
        }
        append("。")
    }
)
