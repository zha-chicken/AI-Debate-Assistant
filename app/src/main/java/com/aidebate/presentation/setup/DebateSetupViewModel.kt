package com.aidebate.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebateSetupUiState(
    val topicTitle: String = "",
    val selectedMode: DebateMode = DebateMode.USER_VS_AI,
    val selectedFormat: DebateFormat = DebateFormat.STRUCTURED,
    val userSide: SpeakerRole? = SpeakerRole.AI_PROPOSITION,
    val enabledProviders: List<ProviderConfig> = emptyList(),
    val providerProposition: AiProvider? = null,
    val modelProposition: String = "",
    val providerOpposition: AiProvider? = null,
    val modelOpposition: String = "",
    val canStart: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class DebateSetupViewModel @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository,
    private val providerConfigRepository: ProviderConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebateSetupUiState())
    val uiState: StateFlow<DebateSetupUiState> = _uiState.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private var topicId: String = ""

    fun initialize(topicId: String) {
        this.topicId = topicId
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            providerConfigRepository.getEnabledConfigs().collect { providers ->
                _uiState.update { state ->
                    state.copy(
                        topicTitle = topic?.title ?: "",
                        enabledProviders = providers,
                        providerProposition = providers.firstOrNull()?.provider,
                        providerOpposition = providers.firstOrNull()?.provider,
                        isLoading = false
                    )
                }
                updateCanStart()
            }
        }
    }

    fun onModeSelected(mode: DebateMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun onFormatSelected(format: DebateFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun onUserSideSelected(side: SpeakerRole) {
        _uiState.update { it.copy(userSide = side) }
    }

    fun onProviderSelected(role: String, provider: AiProvider) {
        _uiState.update {
            if (role == "proposition") it.copy(providerProposition = provider)
            else it.copy(providerOpposition = provider)
        }
        updateCanStart()
    }

    fun onModelChanged(role: String, model: String) {
        _uiState.update {
            if (role == "proposition") it.copy(modelProposition = model)
            else it.copy(modelOpposition = model)
        }
    }

    fun startDebate() {
        val state = _uiState.value
        val session = DebateSession(
            topicId = topicId,
            mode = state.selectedMode,
            format = state.selectedFormat,
            userSide = if (state.selectedMode == DebateMode.USER_VS_AI) state.userSide else null,
            providerProposition = state.providerProposition ?: AiProvider.OPENAI,
            providerOpposition = state.providerOpposition ?: AiProvider.OPENAI,
            modelProposition = state.modelProposition,
            modelOpposition = state.modelOpposition
        )
        viewModelScope.launch {
            debateRepository.createSession(session)
            _sessionId.value = session.id
        }
    }

    private fun updateCanStart() {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                canStart = state.providerProposition != null && state.providerOpposition != null
            )
        }
    }
}
