package com.aidebate.domain.debate

import com.aidebate.domain.model.*
import kotlinx.coroutines.flow.StateFlow

interface DebateOrchestrator {
    val session: StateFlow<DebateSession?>
    val turns: StateFlow<List<DebateTurn>>
    val isThinking: StateFlow<Boolean>
    val contextualState: StateFlow<DebateContextualState>

    suspend fun initialize(session: DebateSession, initialTurns: List<DebateTurn> = emptyList())
    suspend fun submitUserTurn(content: String): DebateTurn
    suspend fun advanceAiTurn(): DebateTurn
    suspend fun requestJudgment(): DebateResult
    fun buildConversationContext(): List<ChatMessage>
    suspend fun endDebate()
}
