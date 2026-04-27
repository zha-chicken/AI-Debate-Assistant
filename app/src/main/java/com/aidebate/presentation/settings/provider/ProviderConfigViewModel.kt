package com.aidebate.presentation.settings.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.data.remote.adapter.ProviderAdapterFactory
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.repository.ProviderConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderConfigUiState(
    val providerName: String = "",
    val provider: AiProvider = AiProvider.OPENAI,
    val apiKey: String = "",
    val modelName: String = "",
    val baseUrl: String = "",
    val showBaseUrl: Boolean = true,
    val isEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val validationState: ValidationState = ValidationState.Idle
) {
    sealed interface ValidationState {
        data object Idle : ValidationState
        data object Validating : ValidationState
        data object Valid : ValidationState
        data class Error(val message: String) : ValidationState
    }
}

@HiltViewModel
class ProviderConfigViewModel @Inject constructor(
    private val providerConfigRepository: ProviderConfigRepository,
    private val adapterFactory: ProviderAdapterFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderConfigUiState())
    val uiState: StateFlow<ProviderConfigUiState> = _uiState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun initialize(providerName: String) {
        viewModelScope.launch {
            val provider = AiProvider.valueOf(providerName)
            val config = providerConfigRepository.getConfig(provider)
            _uiState.value = ProviderConfigUiState(
                providerName = provider.displayName,
                provider = provider,
                apiKey = config?.apiKey ?: "",
                modelName = config?.modelName ?: "",
                baseUrl = config?.baseUrl ?: provider.defaultBaseUrl,
                showBaseUrl = true,
                isEnabled = config?.isEnabled ?: false,
                isLoading = false
            )
        }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(apiKey = value) }
    }

    fun onModelChanged(value: String) {
        _uiState.update { it.copy(modelName = value) }
    }

    fun onBaseUrlChanged(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val config = ProviderConfig(
                provider = state.provider,
                apiKey = state.apiKey,
                modelName = state.modelName,
                baseUrl = state.baseUrl.ifBlank { state.provider.defaultBaseUrl },
                isEnabled = state.isEnabled
            )
            providerConfigRepository.saveConfig(config)
            _uiState.update { it.copy(isSaving = false) }
            _saved.value = true
        }
    }

    fun validateConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(validationState = ProviderConfigUiState.ValidationState.Validating) }
            try {
                val state = _uiState.value
                val config = ProviderConfig(
                    provider = state.provider,
                    apiKey = state.apiKey,
                    modelName = state.modelName,
                    baseUrl = state.baseUrl.ifBlank { state.provider.defaultBaseUrl },
                    isEnabled = true
                )
                val adapter = adapterFactory.getAdapter(state.provider)
                val valid = adapter.validate(config)
                _uiState.update {
                    it.copy(
                        validationState = if (valid) ProviderConfigUiState.ValidationState.Valid
                        else ProviderConfigUiState.ValidationState.Error("Validation failed")
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        validationState = ProviderConfigUiState.ValidationState.Error(
                            e.message ?: "Unknown error"
                        )
                    )
                }
            }
        }
    }
}
