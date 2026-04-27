package com.aidebate.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.repository.ProviderConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val providers: List<ProviderConfig> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val providerConfigRepository: ProviderConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadProviders() {
        viewModelScope.launch {
            providerConfigRepository.getAllConfigs().collect { configs ->
                val allProviders = AiProvider.entries.map { provider ->
                    configs.find { it.provider == provider } ?: ProviderConfig(provider = provider)
                }
                _uiState.value = SettingsUiState(providers = allProviders)
            }
        }
    }
}
