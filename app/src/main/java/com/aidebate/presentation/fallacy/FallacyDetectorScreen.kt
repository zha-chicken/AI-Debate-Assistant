@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.fallacy

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aidebate.domain.model.FallacyReference
import com.aidebate.domain.model.FallacyResult
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

private val HighlightYellow = Color(0x66573D36)
private val HighlightText = Color(0xFFEAF1F4)

@Composable
fun FallacyDetectorScreen(
    onBack: () -> Unit,
    viewModel: FallacyDetectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    AiBackdrop {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(t.fallacyDetectorTitle, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, t.back)
                    }
                },
                actions = {
                    // Reference guide toggle
                    IconButton(onClick = { viewModel.toggleReference() }) {
                        Icon(Icons.Filled.MenuBook, t.referenceGuide)
                    }
                },
                colors = glassTopAppBarColors()
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = uiState.showReference,
                transitionSpec = {
                    if (targetState) slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    else slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                },
                label = "fallacyPanel"
            ) { showRef ->
                if (showRef) {
                    ReferenceGuidePanel(
                        references = FallacyDetectorViewModel.fallacyReferences,
                        selected = uiState.selectedReference,
                        onSelect = { viewModel.selectReference(it) },
                        onClose = { viewModel.toggleReference() }
                    )
                } else {
                    MainContent(uiState, viewModel)
                }
            }
        }
    }
    }
}

// ============================================================
// MAIN CONTENT
// ============================================================

@Composable
private fun MainContent(
    uiState: FallacyDetectorUiState,
    viewModel: FallacyDetectorViewModel
) {
    val t = LocalTranslation.current
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())
    ) {
        Text(t.detectFallacies,
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xs))
        Text(t.fallacyDetectorSub,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(Spacing.xl))

        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { viewModel.onTextChanged(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
            label = { Text(t.argumentLabel) },
            placeholder = { Text(t.argumentPlaceholder) },
            minLines = 6,
            shape = Radii.mediumShape,
            colors = glassTextFieldColors()
        )

        Spacer(Modifier.height(Spacing.md))

        Button(
            onClick = { viewModel.analyze() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = Radii.mediumShape,
            enabled = uiState.inputText.isNotBlank() && !uiState.isAnalyzing,
            colors = glassButtonColors()
        ) {
            if (uiState.isAnalyzing) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(t.analyzing)
            } else {
                Icon(Icons.Filled.Search, null)
                Spacer(Modifier.width(Spacing.sm))
                Text(t.analyzeForFallacies)
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // Results section
        if (!uiState.hasAnalyzed && !uiState.isAnalyzing) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accent = Primary,
                level = GlassCardLevel.PageGroup
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, null, tint = Primary)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            "Paste an argument to analyze",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Rhetorix will identify possible fallacies and explain how to improve the reasoning.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.hasAnalyzed, enter = fadeIn() + expandVertically()) {
            Column {
                ResultsHeader(
                    count = uiState.results.size,
                    onClear = { viewModel.clearResult() }
                )
                Spacer(Modifier.height(Spacing.sm))

                if (uiState.results.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = Radii.mediumShape
                    ) {
                        Row(Modifier.fillMaxWidth().padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen)
                            Spacer(Modifier.width(Spacing.md))
                            Text(t.noFallacies,
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    uiState.results.forEachIndexed { index, result ->
                        StaggeredResultCard(result = result, index = index, delayMs = index * 80L)
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxxl))
    }
}

// ============================================================
// RESULTS HEADER
// ============================================================

@Composable
private fun ResultsHeader(count: Int, onClear: () -> Unit) {
    val t = LocalTranslation.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.results, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (count > 0) {
                Spacer(Modifier.width(Spacing.sm))
                Surface(shape = Radii.smallShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        String.format(t.fallaciesFound, count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp)
                    )
                }
            }
        }
        TextButton(onClick = onClear) {
            Icon(Icons.Filled.Clear, null, Modifier.size(16.dp))
            Text(t.clear)
        }
    }
}

// ============================================================
// RESULT CARD — with severity indicator
// ============================================================

@Composable
private fun StaggeredResultCard(result: FallacyResult, index: Int, delayMs: Long) {
    val t = LocalTranslation.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }

    val severity = computeSeverity(result.name)
    val severityColor = when (severity) {
        "High" -> MaterialTheme.colorScheme.error
        "Medium" -> WarningAmber
        else -> MaterialTheme.colorScheme.outline
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInHorizontally(tween(400, easing = EaseOutCubic)) { it / 3 }
    ) {
        Card(
            shape = Radii.mediumShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            "  #${index + 1}  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(result.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f))
                    // Severity badge
                    Surface(
                        shape = Radii.smallShape,
                        color = severityColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            when (severity) {
                                "High" -> t.severityHigh
                                "Medium" -> t.severityMedium
                                else -> t.severityLow
                            },
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = severityColor
                        )
                    }
                }

                if (result.quotedText.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.md))
                    Surface(
                        color = HighlightYellow,
                        shape = Radii.smallShape
                    ) {
                        Text(
                            "\"${result.quotedText}\"",
                            modifier = Modifier.padding(Spacing.md),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = HighlightText
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))
                Text(result.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
            }
        }
    }
}

// ============================================================
// SEVERITY COMPUTATION (rule-based, no model change needed)
// ============================================================

private val highSeverityFallacies = setOf(
    "Ad Hominem", "Straw Man", "False Dichotomy", "Slippery Slope",
    "Circular Reasoning", "Appeal to Emotion"
)

private fun computeSeverity(name: String): String = when {
    name in highSeverityFallacies -> "High"
    name.contains("Appeal") || name.contains("False") || name.contains("Hasty") -> "Medium"
    else -> "Low"
}

// ============================================================
// REFERENCE GUIDE PANEL
// ============================================================

@Composable
private fun ReferenceGuidePanel(
    references: List<FallacyReference>,
    selected: FallacyReference?,
    onSelect: (FallacyReference?) -> Unit,
    onClose: () -> Unit
) {
    val t = LocalTranslation.current
    Column(modifier = Modifier.fillMaxSize().padding(Spacing.lg)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t.referenceGuide,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, t.close) }
        }
        Text(String.format(t.referenceSubtitle, references.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.height(Spacing.md))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            references.forEachIndexed { index, ref ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 30L)
                    visible = true
                }
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(200))) {
                    Card(
                        onClick = {
                            if (selected?.name == ref.name) onSelect(null) else onSelect(ref)
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = Radii.mediumShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected?.name == ref.name)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(Modifier.padding(Spacing.lg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Text(ref.name, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    if (selected?.name == ref.name) Icons.Filled.ExpandLess
                                    else Icons.Filled.ExpandMore, null,
                                    Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            AnimatedVisibility(visible = selected?.name == ref.name) {
                                Column {
                                    Spacer(Modifier.height(Spacing.sm))
                                    Text(ref.description, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(Spacing.sm))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = Radii.smallShape
                                    ) {
                                        Text(
                                            "\"${ref.example}\"",
                                            modifier = Modifier.padding(Spacing.md),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
