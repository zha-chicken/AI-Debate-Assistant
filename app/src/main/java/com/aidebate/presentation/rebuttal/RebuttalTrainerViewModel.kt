package com.aidebate.presentation.rebuttal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.data.repository.RebuttalTrainerRepositoryImpl
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.RebuttalTrainerRepository
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.domain.repository.TopicRepository
import com.aidebate.presentation.localization.KEY_LANGUAGE
import com.aidebate.presentation.localization.LANG_ENGLISH
import com.aidebate.presentation.localization.translate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RebuttalTrainerUiState(
    val phase: TrainerPhase = TrainerPhase.TOPIC_SELECT,
    val topics: List<DebateTopic> = emptyList(),
    val selectedTopicId: String? = null,
    val selectedTopicTitle: String = "",
    val userSide: String = "AGAINST",
    val difficulty: String = "medium",
    val timeLimitSec: Int = 60,
    val sessionId: String? = null,
    val promptArgument: String = "",
    val userResponse: String = "",
    val timeRemainingSec: Int = 60,
    val isTimerRunning: Boolean = false,
    val isGenerating: Boolean = false,
    val isScoring: Boolean = false,
    val currentAttempt: RebuttalAttempt? = null,
    val attempts: List<RebuttalAttempt> = emptyList(),
    val sessions: List<RebuttalSession> = emptyList(),
    val isExplaining: Boolean = false,
    val explanation: RebuttalExplanation? = null,
    val chatMessages: List<RebuttalChatMessage> = emptyList(),
    val chatInput: String = "",
    val reviewingSessionId: String? = null, // non-null when viewing history
    val error: String? = null
)

enum class TrainerPhase {
    TOPIC_SELECT, SETUP, READY, RESPONDING, SCORING, RESULT
}

@HiltViewModel
class RebuttalTrainerViewModel @Inject constructor(
    private val repository: RebuttalTrainerRepository,
    private val topicRepository: TopicRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RebuttalTrainerUiState())
    val uiState: StateFlow<RebuttalTrainerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                topicRepository.getAllTopics(),
                settingsRepository.observeString(KEY_LANGUAGE).map { it ?: LANG_ENGLISH }
            ) { topics, lang ->
                topics.map { it.translate(lang) }
            }.collect { topics ->
                _uiState.update { it.copy(topics = topics) }
            }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _uiState.update { it.copy(sessions = sessions) }
            }
        }
    }

    fun selectTopic(topicId: String, topicTitle: String) {
        _uiState.update { it.copy(selectedTopicId = topicId, selectedTopicTitle = topicTitle, phase = TrainerPhase.SETUP) }
    }

    fun setSide(side: String) {
        _uiState.update { it.copy(userSide = side) }
    }

    fun setDifficulty(difficulty: String) {
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun setTimeLimit(seconds: Int) {
        _uiState.update { it.copy(timeLimitSec = seconds, timeRemainingSec = seconds) }
    }

    fun startSession() {
        val state = _uiState.value
        if (state.selectedTopicId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                val session = RebuttalSession(
                    topicId = state.selectedTopicId,
                    topicTitle = state.selectedTopicTitle,
                    userSide = state.userSide,
                    difficulty = state.difficulty
                )
                repository.createSession(session)

                val impl = repository as RebuttalTrainerRepositoryImpl
                val prompt = impl.generateRebuttalPrompt(
                    state.selectedTopicTitle, state.userSide, state.difficulty
                )
                _uiState.update {
                    it.copy(
                        isGenerating = false, sessionId = session.id,
                        promptArgument = prompt, phase = TrainerPhase.READY
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = e.message ?: "Failed to generate prompt") }
            }
        }
    }

    fun startTimer() {
        val state = _uiState.value
        _uiState.update { it.copy(phase = TrainerPhase.RESPONDING, isTimerRunning = true, timeRemainingSec = state.timeLimitSec) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = state.timeLimitSec
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(timeRemainingSec = remaining) }
            }
            _uiState.update { it.copy(isTimerRunning = false) }
        }
    }

    fun onResponseChanged(response: String) {
        _uiState.update { it.copy(userResponse = response) }
    }

    fun submitRebuttal() {
        val state = _uiState.value
        if (state.userResponse.isBlank() || state.sessionId == null) return
        timerJob?.cancel()
        val elapsedMs = (state.timeLimitSec - state.timeRemainingSec) * 1000L
        _uiState.update { it.copy(isTimerRunning = false, phase = TrainerPhase.SCORING, isScoring = true) }
        viewModelScope.launch {
            try {
                val impl = repository as RebuttalTrainerRepositoryImpl
                val scored = impl.scoreRebuttal(state.sessionId, state.promptArgument, state.userResponse)
                val attempt = scored.copy(
                    sessionId = state.sessionId,
                    promptArgument = state.promptArgument,
                    userResponse = state.userResponse,
                    timeLimitSec = state.timeLimitSec,
                    timeTakenMs = elapsedMs
                )
                repository.saveAttempt(attempt)
                _uiState.update { it.copy(isScoring = false, currentAttempt = attempt, phase = TrainerPhase.RESULT) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScoring = false, error = e.message ?: "Scoring failed") }
            }
        }
    }

    fun requestExplanation() {
        val state = _uiState.value
        val attempt = state.currentAttempt ?: return
        if (state.promptArgument.isBlank() || state.userResponse.isBlank()) return
        _uiState.update { it.copy(isExplaining = true) }
        viewModelScope.launch {
            try {
                val impl = repository as RebuttalTrainerRepositoryImpl
                val explanation = impl.generateExplanation(
                    topicTitle = state.selectedTopicTitle,
                    promptArgument = state.promptArgument,
                    userResponse = state.userResponse,
                    attempt = attempt
                )
                _uiState.update {
                    it.copy(isExplaining = false, explanation = explanation,
                        chatMessages = listOf(RebuttalChatMessage("ai",
                            "Here's a detailed breakdown of your scores. Ask me anything about your performance!")))
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExplaining = false, error = e.message ?: "Failed to get explanation") }
            }
        }
    }

    fun onChatInputChanged(text: String) {
        _uiState.update { it.copy(chatInput = text) }
    }

    fun sendChatMessage() {
        val state = _uiState.value
        val text = state.chatInput.trim()
        if (text.isBlank()) return
        val userMsg = RebuttalChatMessage("user", text)
        _uiState.update { it.copy(chatInput = "", chatMessages = state.chatMessages + userMsg) }
        viewModelScope.launch {
            try {
                val impl = repository as RebuttalTrainerRepositoryImpl
                val reply = impl.chatAboutRebuttal(
                    topicTitle = state.selectedTopicTitle,
                    promptArgument = state.promptArgument,
                    userResponse = state.userResponse,
                    attempt = state.currentAttempt ?: return@launch,
                    messages = _uiState.value.chatMessages
                )
                _uiState.update { it.copy(chatMessages = _uiState.value.chatMessages + reply) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Chat failed: ${e.message}") }
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(explanation = null, chatMessages = emptyList(), chatInput = "") }
    }

    fun newRound() {
        _uiState.update { it.copy(phase = TrainerPhase.SETUP, promptArgument = "", userResponse = "", currentAttempt = null, explanation = null, chatMessages = emptyList(), chatInput = "") }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            repository.getAttempts(sessionId).collect { attempts ->
                _uiState.update { it.copy(attempts = attempts, sessionId = sessionId) }
            }
        }
    }

    fun backToTopics() {
        _uiState.update { it.copy(phase = TrainerPhase.TOPIC_SELECT, selectedTopicId = null, promptArgument = "", userResponse = "", currentAttempt = null, explanation = null, chatMessages = emptyList(), chatInput = "", reviewingSessionId = null) }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            val session = repository.getSession(sessionId) ?: return@launch
            repository.getAttempts(sessionId).collect { attempts ->
                val best = attempts.maxByOrNull { it.totalScore }
                _uiState.update {
                    it.copy(
                        reviewingSessionId = sessionId,
                        selectedTopicTitle = session.topicTitle,
                        attempts = attempts,
                        currentAttempt = best,
                        phase = if (best != null) TrainerPhase.RESULT else TrainerPhase.TOPIC_SELECT,
                        explanation = null, chatMessages = emptyList()
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
