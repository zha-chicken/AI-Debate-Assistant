package com.aidebate.presentation.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.DebateResult
import com.aidebate.domain.model.DebateSession
import com.aidebate.domain.model.DebateTurn
import com.aidebate.domain.model.RecommendationFeedback
import com.aidebate.domain.model.RecommendationFeedbackReasonType
import com.aidebate.domain.model.RecommendationFeedbackSentiment
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.RecommendationRepository
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
    val topicCategory: String = "",
    val recommendationFeedback: RecommendationFeedback? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DebateResultViewModel @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository,
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateResultUiState())
    val uiState: StateFlow<DebateResultUiState> = _uiState.asStateFlow()

    fun initialize(sessionId: String) {
        viewModelScope.launch {
            val session = debateRepository.getSession(sessionId).first() ?: return@launch
            val topic = topicRepository.getTopic(session.topicId)
            combine(
                debateRepository.getTurns(sessionId),
                debateRepository.getResult(sessionId),
                recommendationRepository.observeFeedback(sessionId)
            ) { turns, result, feedback -> Triple(turns, result, feedback) }
                .collect { (turns, result, feedback) ->
                    _uiState.value = DebateResultUiState(
                        topicTitle = topic?.title ?: "",
                        session = session,
                        turns = turns,
                        result = result,
                        topicCategory = topic?.category ?: "",
                        recommendationFeedback = feedback,
                        isLoading = false
                    )
                }
        }
    }

    fun recordRecommendationFeedback(
        sentiment: RecommendationFeedbackSentiment,
        reasonType: RecommendationFeedbackReasonType
    ) {
        val state = _uiState.value
        val session = state.session ?: return
        viewModelScope.launch {
            recommendationRepository.recordFeedback(
                sessionId = session.id,
                topicTitle = state.topicTitle,
                category = state.topicCategory,
                sentiment = sentiment,
                reasonType = reasonType
            )
        }
    }
}
