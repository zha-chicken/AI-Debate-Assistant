package com.aidebate.presentation.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.DebateResult
import com.aidebate.domain.model.DebateSession
import com.aidebate.domain.model.DebateTurn
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateResultUiState(
    val topicTitle: String = "",
    val session: DebateSession? = null,
    val turns: List<DebateTurn> = emptyList(),
    val result: DebateResult? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DebateResultViewModel @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateResultUiState())
    val uiState: StateFlow<DebateResultUiState> = _uiState.asStateFlow()

    fun initialize(sessionId: String) {
        viewModelScope.launch {
            val session = debateRepository.getSession(sessionId).first() ?: return@launch
            val topic = topicRepository.getTopic(session.topicId)
            combine(
                debateRepository.getTurns(sessionId),
                debateRepository.getResult(sessionId)
            ) { turns, result -> turns to result }
                .collect { (turns, result) ->
                    _uiState.value = DebateResultUiState(
                        topicTitle = topic?.title ?: "",
                        session = session,
                        turns = turns,
                        result = result,
                        isLoading = false
                    )
                }
        }
    }
}
