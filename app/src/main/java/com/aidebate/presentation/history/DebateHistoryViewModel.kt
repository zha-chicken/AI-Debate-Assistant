package com.aidebate.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.HistoryItem
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.RebuttalTrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateHistoryUiState(
    val items: List<HistoryItem> = emptyList(),
    val isEmpty: Boolean = true
)

@HiltViewModel
class DebateHistoryViewModel @Inject constructor(
    private val debateRepository: DebateRepository,
    private val rebuttalRepository: RebuttalTrainerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateHistoryUiState())
    val uiState: StateFlow<DebateHistoryUiState> = _uiState.asStateFlow()

    private val _debateItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    private val _rebuttalItems = MutableStateFlow<List<HistoryItem>>(emptyList())

    fun loadSessions() {
        // Collect debate sessions
        viewModelScope.launch {
            debateRepository.getAllSessions().collect { sessions ->
                _debateItems.value = sessions.map { HistoryItem.Debate(it) }
            }
        }
        // Collect rebuttal sessions with their best scores
        viewModelScope.launch {
            rebuttalRepository.getAllSessions().collect { sessions ->
                val items = sessions.map { session ->
                    val attempts = rebuttalRepository.getAttempts(session.id).first()
                    HistoryItem.Rebuttal(
                        session = session,
                        bestScore = attempts.maxOfOrNull { it.totalScore },
                        attemptCount = attempts.size
                    )
                }
                _rebuttalItems.value = items
            }
        }
        // Merge both lists sorted by date
        viewModelScope.launch {
            combine(_debateItems, _rebuttalItems) { debates, rebuttals ->
                (debates + rebuttals).sortedByDescending { it.createdAt }
            }.collect { items ->
                _uiState.value = DebateHistoryUiState(
                    items = items,
                    isEmpty = items.isEmpty()
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
