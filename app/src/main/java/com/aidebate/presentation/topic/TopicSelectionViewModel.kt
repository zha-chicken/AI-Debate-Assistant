package com.aidebate.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.DebateTopic
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicSelectionUiState(
    val categories: List<String> = emptyList(),
    val topicsByCategory: Map<String, List<DebateTopic>> = emptyMap(),
    val isLoading: Boolean = false,
    val customTopicResult: String? = null // topicId of newly created topic
)

@HiltViewModel
class TopicSelectionViewModel @Inject constructor(
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TopicSelectionUiState(isLoading = true))
    val uiState: StateFlow<TopicSelectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            topicRepository.seedPredefinedTopics()
        }
    }

    fun loadTopics() {
        viewModelScope.launch {
            topicRepository.getAllTopics().collect { topics ->
                val byCategory = topics.groupBy { it.category.ifBlank { "General" } }
                    .toSortedMap()
                _uiState.value = TopicSelectionUiState(
                    categories = byCategory.keys.toList(),
                    topicsByCategory = byCategory,
                    isLoading = false
                )
            }
        }
    }

    fun addCustomTopic(title: String) {
        viewModelScope.launch {
            val topic = DebateTopic(
                title = title,
                category = "Custom",
                description = "",
                isPredefined = false
            )
            topicRepository.saveTopic(topic)
            _uiState.update { it.copy(customTopicResult = topic.id) }
        }
    }

    fun clearCustomTopicResult() {
        _uiState.update { it.copy(customTopicResult = null) }
    }
}
