package com.aidebate.presentation.debate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.debate.DebateOrchestrator
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateUiState(
    val topicTitle: String = "",
    val session: DebateSession? = null,
    val mode: DebateMode = DebateMode.USER_VS_AI,
    val format: DebateFormat = DebateFormat.STRUCTURED,
    val turns: List<DebateTurn> = emptyList(),
    val contextualState: DebateContextualState = DebateContextualState.WaitingForUserInput,
    val isThinking: Boolean = false,
    val currentPhase: StructuredPhase? = null,
    val userInputText: String = "",
    val error: String? = null,
    val result: DebateResult? = null,
    val isInitializing: Boolean = true
)

@HiltViewModel
class DebateViewModel @Inject constructor(
    private val orchestrator: DebateOrchestrator,
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateUiState())
    val uiState: StateFlow<DebateUiState> = _uiState.asStateFlow()

    fun initialize(sessionId: String) {
        viewModelScope.launch {
            debateRepository.getSession(sessionId).collect { session ->
                if (session != null) {
                    val topic = topicRepository.getTopic(session.topicId)
                    _uiState.update {
                        it.copy(
                            session = session,
                            topicTitle = topic?.title ?: "",
                            mode = session.mode,
                            format = session.format,
                            isInitializing = false
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            debateRepository.getTurns(sessionId).collect { turns ->
                _uiState.update {
                    it.copy(
                        turns = turns,
                        currentPhase = turns.lastOrNull()?.phase
                    )
                }
            }
        }

        viewModelScope.launch {
            orchestrator.isThinking.collect { thinking ->
                _uiState.update { it.copy(isThinking = thinking) }
            }
        }

        viewModelScope.launch {
            orchestrator.contextualState.collect { state ->
                _uiState.update { it.copy(contextualState = state) }
            }
        }

        viewModelScope.launch {
            debateRepository.getResult(sessionId).collect { result ->
                _uiState.update { it.copy(result = result) }
            }
        }

        viewModelScope.launch {
            delay(100) // let flows init
            val session = debateRepository.getSession(sessionId).first() ?: return@launch
            val turns = debateRepository.getTurns(sessionId).first()
            orchestrator.initialize(session, turns)
        }
    }

    fun onUserInputChanged(text: String) {
        _uiState.update { it.copy(userInputText = text, error = null) }
    }

    fun submitUserTurn() {
        val text = _uiState.value.userInputText.trim()
        if (text.isBlank()) return

        _uiState.update { it.copy(userInputText = "", error = null) }
        viewModelScope.launch {
            try {
                orchestrator.submitUserTurn(text)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error sending message") }
            }
        }
    }

    fun onTapToAdvance() {
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                orchestrator.advanceAiTurn()
            } catch (e: Exception) {
                if (e.message?.contains("already completed") == true) return@launch
                _uiState.update { it.copy(error = e.message ?: "Error advancing turn") }
            }
        }
    }

    fun requestJudgment() {
        viewModelScope.launch {
            try {
                orchestrator.requestJudgment()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error requesting judgment") }
            }
        }
    }

    fun endDebate() {
        viewModelScope.launch {
            orchestrator.endDebate()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
