package com.aidebate.presentation.constructive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.ConstructiveAnalysisIssue
import com.aidebate.domain.repository.ConstructiveAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConstructiveAnalysisUiState(
    val inputText: String = "",
    val issues: List<ConstructiveAnalysisIssue> = emptyList(),
    val isAnalyzing: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ConstructiveAnalysisViewModel @Inject constructor(
    private val repository: ConstructiveAnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConstructiveAnalysisUiState())
    val uiState: StateFlow<ConstructiveAnalysisUiState> = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun analyze() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            try {
                val issues = repository.analyzeConstructive(text)
                _uiState.update {
                    it.copy(
                        issues = issues,
                        isAnalyzing = false,
                        hasAnalyzed = true,
                        errorMessage = null,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        hasAnalyzed = true,
                        errorMessage = error.message ?: "Analysis failed. Please try again.",
                    )
                }
            }
        }
    }

    fun clearResults() {
        _uiState.update { it.copy(issues = emptyList(), hasAnalyzed = false, errorMessage = null) }
    }
}
