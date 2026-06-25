package com.aidebate.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.MBTIType
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.RecommendationRepository
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.presentation.localization.KEY_LANGUAGE
import com.aidebate.presentation.localization.LANG_ENGLISH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val currentLanguage: String = LANG_ENGLISH,
    val selectedMbti: MBTIType? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val providerConfigRepository: ProviderConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val recommendationRepository: RecommendationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.observeString(KEY_LANGUAGE).map { it ?: LANG_ENGLISH },
                recommendationRepository.observeMbti()
            ) { lang, mbti -> lang to mbti }
                .collect { (lang, mbti) ->
                    _uiState.update { it.copy(currentLanguage = lang, selectedMbti = mbti) }
                }
        }
    }

    fun loadProviders() {
        viewModelScope.launch {
            providerConfigRepository.getAllConfigs().collect { configs ->
                val allProviders = AiProvider.entries.map { provider ->
                    configs.find { it.provider == provider } ?: ProviderConfig(provider = provider)
                }
                _uiState.value = SettingsUiState(
                    providers = allProviders,
                    currentLanguage = _uiState.value.currentLanguage,
                    selectedMbti = _uiState.value.selectedMbti
                )
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setString(KEY_LANGUAGE, code)
        }
    }

    fun setMbti(type: MBTIType?) {
        viewModelScope.launch {
            recommendationRepository.setMbti(type)
        }
    }
}
