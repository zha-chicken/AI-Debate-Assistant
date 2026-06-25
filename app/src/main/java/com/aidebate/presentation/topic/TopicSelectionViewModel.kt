package com.aidebate.presentation.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.ContentSafetySource
import com.aidebate.domain.model.DebateTopic
import com.aidebate.domain.repository.ContentSafetyRepository
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.domain.repository.TopicRepository
import com.aidebate.presentation.localization.KEY_LANGUAGE
import com.aidebate.presentation.localization.LANG_ENGLISH
import com.aidebate.presentation.localization.translate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicSelectionUiState(
    val categories: List<String> = emptyList(),
    val topicsByCategory: Map<String, List<DebateTopic>> = emptyMap(),
    val topicUsageCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val customTopicResult: String? = null, // topicId of newly created topic
    val customTopicError: String? = null
)

@HiltViewModel
class TopicSelectionViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val settingsRepository: SettingsRepository,
    private val debateRepository: DebateRepository,
    private val contentSafetyRepository: ContentSafetyRepository
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
            combine(
                topicRepository.getAllTopics(),
                settingsRepository.observeString(KEY_LANGUAGE).map { it ?: LANG_ENGLISH },
                debateRepository.getAllSessions()
            ) { rawTopics, lang, sessions ->
                val usageByTitle = sessions
                    .groupingBy { normalizeTitle(it.topicTitle) }
                    .eachCount()
                val usageById = rawTopics.associate { topic ->
                    topic.id to (usageByTitle[normalizeTitle(topic.title)] ?: 0)
                }
                rawTopics.map { it.translate(lang) } to usageById
            }.collect { (topics, usageById) ->
                val byCategory = topics.groupBy { it.category.ifBlank { "General" } }
                    .toSortedMap()
                _uiState.value = TopicSelectionUiState(
                    categories = byCategory.keys.toList(),
                    topicsByCategory = byCategory,
                    topicUsageCounts = usageById,
                    isLoading = false
                )
            }
        }
    }

    fun addCustomTopic(title: String) {
        viewModelScope.launch {
            try {
                contentSafetyRepository.assertSafe(
                    text = title,
                    source = ContentSafetySource.USER_PROMPT
                )
                val topic = DebateTopic(
                    title = title,
                    category = "Custom",
                    description = "",
                    isPredefined = false
                )
                topicRepository.saveTopic(topic)
                _uiState.update { it.copy(customTopicResult = topic.id, customTopicError = null) }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        customTopicError = error.message ?: "内容安全检测服务异常，请稍后重试"
                    )
                }
            }
        }
    }

    fun clearCustomTopicResult() {
        _uiState.update { it.copy(customTopicResult = null) }
    }

    fun clearCustomTopicError() {
        _uiState.update { it.copy(customTopicError = null) }
    }

    private fun normalizeTitle(title: String): String =
        title.trim().trimEnd('?').lowercase()
}
