package com.aidebate.domain.model

import java.util.UUID

enum class DebateMode { USER_VS_AI, AI_VS_AI, USER_VS_USER }

enum class DebateFormat { STRUCTURED, FREE_FLOW }

enum class SpeakerRole { USER, AI_PROPOSITION, AI_OPPOSITION, MODERATOR }

enum class StructuredPhase { OPENING, REBUTTAL, CLOSING }

enum class DebateDifficulty { EASY, MEDIUM, HARD }

enum class HighlightType { STRONG_ARGUMENT, WEAK_EVIDENCE, LOGICAL_FALLACY, CRITICAL_FLAW, NOTABLE_INSIGHT }

data class TurnScore(
    val overall: Int,
    val rationale: String,
    val scoredBy: SpeakerRole? = null
)

data class ArgumentHighlight(
    val type: HighlightType,
    val quotedText: String,
    val label: String
)

enum class AiProvider(val displayName: String, val defaultBaseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/"),
    GROQ("Groq", "https://api.groq.com/"),
    OLLAMA("Ollama", "http://localhost:11434/");
}

data class ProviderConfig(
    val provider: AiProvider,
    val apiKey: String = "",
    val modelName: String = "",
    val baseUrl: String = provider.defaultBaseUrl,
    val isEnabled: Boolean = false
)

data class DebateTopic(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String = "",
    val description: String = "",
    val isPredefined: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DebateSession(
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val mode: DebateMode = DebateMode.USER_VS_AI,
    val format: DebateFormat = DebateFormat.STRUCTURED,
    val userSide: SpeakerRole? = null,
    val providerProposition: AiProvider = AiProvider.OPENAI,
    val providerOpposition: AiProvider = AiProvider.OPENAI,
    val modelProposition: String = "",
    val modelOpposition: String = "",
    val status: SessionStatus = SessionStatus.ACTIVE,
    val currentPhase: StructuredPhase? = null,
    val difficulty: DebateDifficulty = DebateDifficulty.MEDIUM,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SessionStatus { ACTIVE, COMPLETED, ABANDONED }

data class DebateTurn(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val speakerRole: SpeakerRole,
    val content: String,
    val phase: StructuredPhase? = null,
    val turnIndex: Int = 0,
    val providerUsed: AiProvider? = null,
    val modelUsed: String? = null,
    val score: TurnScore? = null,
    val highlights: List<ArgumentHighlight>? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DebateResult(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val winner: SpeakerRole? = null,
    val summary: String = "",
    val judgedByProvider: AiProvider? = null,
    val judgedByModel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class DebateSessionSummary(
    val id: String,
    val topicTitle: String,
    val mode: DebateMode,
    val format: DebateFormat,
    val status: SessionStatus,
    val userSide: SpeakerRole? = null,
    val turnCount: Int,
    val createdAt: Long
)

sealed interface HistoryItem {
    val id: String
    val title: String
    val createdAt: Long
    val typeLabel: String

    data class Debate(val summary: DebateSessionSummary) : HistoryItem {
        override val id get() = summary.id
        override val title get() = summary.topicTitle
        override val createdAt get() = summary.createdAt
        override val typeLabel get() = "Debate"
    }

    data class Rebuttal(val session: RebuttalSession, val bestScore: Int?, val attemptCount: Int) : HistoryItem {
        override val id get() = session.id
        override val title get() = session.topicTitle
        override val createdAt get() = session.createdAt
        override val typeLabel get() = "Rebuttal"
    }
}
