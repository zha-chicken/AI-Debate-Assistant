package com.aidebate.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.presentation.localization.KEY_LANGUAGE
import com.aidebate.presentation.localization.LANG_ENGLISH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val currentLanguage: String = LANG_ENGLISH
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val providerConfigRepository: ProviderConfigRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeString(KEY_LANGUAGE)
                .map { it ?: LANG_ENGLISH }
                .collect { lang ->
                    _uiState.update { it.copy(currentLanguage = lang) }
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
                    currentLanguage = _uiState.value.currentLanguage
                )
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            settingsRepository.setString(KEY_LANGUAGE, code)
        }
    }
}
