package com.aidebate.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.DebateSessionSummary
import com.aidebate.domain.repository.DebateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateHistoryUiState(
    val sessions: List<DebateSessionSummary> = emptyList(),
    val isEmpty: Boolean = true
)

@HiltViewModel
class DebateHistoryViewModel @Inject constructor(
    private val debateRepository: DebateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateHistoryUiState())
    val uiState: StateFlow<DebateHistoryUiState> = _uiState.asStateFlow()

    fun loadSessions() {
        viewModelScope.launch {
            debateRepository.getAllSessions().collect { sessions ->
                _uiState.value = DebateHistoryUiState(
                    sessions = sessions,
                    isEmpty = sessions.isEmpty()
                )
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            debateRepository.deleteSession(sessionId)
        }
    }
}
