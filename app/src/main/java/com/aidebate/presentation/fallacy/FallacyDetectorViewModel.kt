package com.aidebate.presentation.fallacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.data.repository.RebuttalTrainerRepositoryImpl
import com.aidebate.domain.model.FallacyReference
import com.aidebate.domain.model.FallacyResult
import com.aidebate.domain.repository.RebuttalTrainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FallacyDetectorUiState(
    val inputText: String = "",
    val results: List<FallacyResult> = emptyList(),
    val isAnalyzing: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val showReference: Boolean = false,
    val selectedReference: FallacyReference? = null,
    val error: String? = null
)

@HiltViewModel
class FallacyDetectorViewModel @Inject constructor(
    private val repository: RebuttalTrainerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FallacyDetectorUiState())
    val uiState: StateFlow<FallacyDetectorUiState> = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun analyze() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, results = emptyList()) }
            try {
                val impl = repository as RebuttalTrainerRepositoryImpl
                val results = impl.analyzeFallacies(text)
                _uiState.update { it.copy(isAnalyzing = false, hasAnalyzed = true, results = results) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isAnalyzing = false, error = e.message ?: "Analysis failed")
                }
            }
        }
    }

    fun toggleReference() {
        _uiState.update { it.copy(showReference = !it.showReference) }
    }

    fun selectReference(ref: FallacyReference?) {
        _uiState.update { it.copy(selectedReference = ref) }
    }

    fun clearResult() {
        _uiState.update { it.copy(results = emptyList(), hasAnalyzed = false, inputText = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        val fallacyReferences = listOf(
            FallacyReference("Ad Hominem", "Attacking the person instead of the argument.", "You're wrong because you're not a scientist."),
            FallacyReference("Straw Man", "Misrepresenting an argument to make it easier to attack.", "You want to reduce military spending? So you want to leave the country defenseless!"),
            FallacyReference("False Dichotomy", "Presenting only two options when more exist.", "Either you support this policy, or you don't care about children."),
            FallacyReference("Slippery Slope", "Claiming a small step will inevitably lead to extreme consequences.", "If we allow this small tax increase, soon the government will take all our money."),
            FallacyReference("Circular Reasoning", "The conclusion is assumed in the premise.", "This law is just because it's the right thing to do."),
            FallacyReference("Hasty Generalization", "Making a broad claim from insufficient evidence.", "My neighbor is rude, so people from that city must all be rude."),
            FallacyReference("Appeal to Authority", "Claiming something is true because an authority figure said so.", "This product is the best because a celebrity endorses it."),
            FallacyReference("Red Herring", "Introducing an irrelevant topic to distract from the issue.", "Why worry about climate change when there are people without jobs?"),
            FallacyReference("Bandwagon", "Claiming something is true because many people believe it.", "Everyone else is doing it, so it must be right."),
            FallacyReference("False Cause", "Assuming correlation implies causation.", "Crime rates went up after the mayor was elected, so the mayor caused the increase."),
            FallacyReference("Appeal to Emotion", "Manipulating emotions instead of making a logical argument.", "Think of the children! You must support this bill."),
            FallacyReference("No True Scotsman", "Dismissing counterexamples by redefining criteria arbitrarily.", "No real patriot would question this policy."),
            FallacyReference("Tu Quoque", "Dismissing criticism by pointing out the critic's hypocrisy.", "You tell me not to smoke, but you used to smoke yourself!"),
            FallacyReference("Begging the Question", "Assuming the conclusion in one of the premises.", "This medicine works because it has healing properties."),
            FallacyReference("Equivocation", "Using an ambiguous term in multiple senses within an argument.", "A feather is light. What is light cannot be dark. Therefore a feather cannot be dark.")
        )
    }
}
