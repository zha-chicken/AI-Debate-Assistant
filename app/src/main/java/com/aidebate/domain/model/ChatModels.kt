package com.aidebate.domain.model

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatConfig(
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1024
)

data class ChatResult(
    val content: String,
    val tokenUsage: TokenUsage? = null,
    val finishReason: String? = null
)

data class TokenUsage(val input: Int, val output: Int)

sealed interface DebateContextualState {
    data object WaitingForUserInput : DebateContextualState
    data class WaitingForAiTurn(
        val speakerRole: SpeakerRole,
        val provider: AiProvider,
        val model: String
    ) : DebateContextualState
    data class WaitingForTap(
        val nextSpeaker: SpeakerRole,
        val nextProvider: AiProvider
    ) : DebateContextualState
    data object DebateCompleted : DebateContextualState
    data object Judging : DebateContextualState
    data class Error(val message: String) : DebateContextualState
}
