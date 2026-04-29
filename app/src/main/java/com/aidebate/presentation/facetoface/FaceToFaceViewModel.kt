package com.aidebate.presentation.facetoface

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.debate.DebateOrchestrator
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FaceToFaceUiState(
    val topicTitle: String = "",
    val sessionId: String? = null,
    val session: DebateSession? = null,
    val turnIndex: Int = 0,           // 0..5 — which of the 6 turns we're on
    val currentPhase: StructuredPhase = StructuredPhase.OPENING,
    val currentSpeaker: SpeakerRole = SpeakerRole.AI_PROPOSITION, // P1 = PRO, P2 = CON
    val turns: List<DebateTurn> = emptyList(),
    val userInput: String = "",
    val showPassOverlay: Boolean = false,
    val isJudging: Boolean = false,
    val result: DebateResult? = null,
    val isSaving: Boolean = false,
    val isInitializing: Boolean = true,
    val error: String? = null
) {
    val isComplete: Boolean get() = turnIndex >= 6
    val turnCount: Int get() = minOf(turnIndex + 1, 6)
    val isLastTurn: Boolean get() = turnIndex == 5
    val isPlayer1Turn: Boolean get() = currentSpeaker == SpeakerRole.AI_PROPOSITION
}

private val TURN_SPEAKERS = listOf(
    SpeakerRole.AI_PROPOSITION, // 0: P1 Opening
    SpeakerRole.AI_OPPOSITION,  // 1: P2 Opening
    SpeakerRole.AI_PROPOSITION, // 2: P1 Rebuttal
    SpeakerRole.AI_OPPOSITION,  // 3: P2 Rebuttal
    SpeakerRole.AI_PROPOSITION, // 4: P1 Closing
    SpeakerRole.AI_OPPOSITION,  // 5: P2 Closing
)

private fun phaseForTurn(index: Int): StructuredPhase = when (index) {
    0, 1 -> StructuredPhase.OPENING
    2, 3 -> StructuredPhase.REBUTTAL
    4, 5 -> StructuredPhase.CLOSING
    else -> StructuredPhase.CLOSING
}

@HiltViewModel
class FaceToFaceViewModel @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository,
    private val orchestrator: DebateOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(FaceToFaceUiState())
    val uiState: StateFlow<FaceToFaceUiState> = _uiState.asStateFlow()

    fun initialize(sessionId: String) {
        viewModelScope.launch {
            debateRepository.getSession(sessionId).collect { session ->
                if (session != null) {
                    val topic = topicRepository.getTopic(session.topicId)
                    _uiState.update {
                        it.copy(
                            session = session,
                            sessionId = session.id,
                            topicTitle = topic?.title ?: "",
                            isInitializing = false
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            debateRepository.getTurns(sessionId).collect { turns ->
                val idx = turns.size // next turn index
                val speaker = if (idx < TURN_SPEAKERS.size) TURN_SPEAKERS[idx] else SpeakerRole.AI_PROPOSITION
                val phase = phaseForTurn(idx)
                val isComplete = idx >= 6
                val hasResult = turns.any { it.speakerRole == SpeakerRole.MODERATOR }

                _uiState.update {
                    it.copy(
                        turns = turns,
                        turnIndex = idx,
                        currentSpeaker = speaker,
                        currentPhase = phase,
                        userInput = if (isComplete && !hasResult) "" else it.userInput
                    )
                }

                // If turns are already complete, load result
                if (isComplete) {
                    debateRepository.getResult(sessionId).collect { result ->
                        _uiState.update { it.copy(result = result) }
                    }
                }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(userInput = text, error = null) }
    }

    fun submitTurn() {
        val state = _uiState.value
        val text = state.userInput.trim()
        if (text.isBlank() || state.sessionId == null || state.isComplete) return
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, userInput = "") }

        viewModelScope.launch {
            try {
                val turn = DebateTurn(
                    sessionId = state.sessionId,
                    speakerRole = state.currentSpeaker,
                    content = text,
                    phase = state.currentPhase,
                    turnIndex = state.turnIndex
                )
                debateRepository.addTurn(turn)

                _uiState.update { it.copy(isSaving = false, showPassOverlay = true) }
                kotlinx.coroutines.delay(1500)
                _uiState.update { it.copy(showPassOverlay = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save turn") }
            }
        }
    }

    fun dismissPassOverlay() {
        _uiState.update { it.copy(showPassOverlay = false) }
    }

    fun requestJudgment() {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.isJudging) return

        _uiState.update { it.copy(isJudging = true, error = null) }

        viewModelScope.launch {
            try {
                val currentTurns = debateRepository.getTurns(session.id).first()
                orchestrator.initialize(session, currentTurns)
                orchestrator.requestJudgment()

                // Re-observe result
                debateRepository.getResult(session.id).collect { result ->
                    if (result != null) {
                        _uiState.update { it.copy(result = result, isJudging = false) }
                        return@collect
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isJudging = false, error = e.message ?: "Judgment failed")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
