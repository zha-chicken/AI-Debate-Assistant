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
import com.aidebate.presentation.theme.SuccessGreen
import com.aidebate.presentation.theme.WarningAmber
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

val HighlightYellow = Color(0xFFFFF9C4)
val HighlightText = Color(0xFF4A3800)

@Composable
fun FallacyDetectorScreen(
    onBack: () -> Unit,
    viewModel: FallacyDetectorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fallacy Detector", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleReference() }) {
                        Icon(Icons.Filled.MenuBook, "Reference Guide")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = uiState.showReference,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(400)) { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -it / 4 })
                },
                label = "main"
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

            // Error
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.error ?: "", Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    uiState: FallacyDetectorUiState,
    viewModel: FallacyDetectorViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Detect Logical Fallacies",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Paste or type an argument to scan for logical fallacies using AI.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = uiState.inputText,
            onValueChange = { viewModel.onTextChanged(it) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 300.dp),
            label = { Text("Argument text to analyze") },
            placeholder = { Text("Paste or type an argument here...") },
            minLines = 6,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.analyze() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = uiState.inputText.isNotBlank() && !uiState.isAnalyzing
        ) {
            if (uiState.isAnalyzing) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Filled.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze for Fallacies")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Results section
        AnimatedVisibility(visible = uiState.hasAnalyzed, enter = fadeIn() + expandVertically()) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (uiState.results.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    " ${uiState.results.size} found ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.clearResult() }) {
                        Icon(Icons.Filled.Clear, null, Modifier.size(16.dp))
                        Text("Clear")
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (uiState.results.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen)
                            Spacer(Modifier.width(12.dp))
                            Text("No logical fallacies detected!",
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    uiState.results.forEachIndexed { index, result ->
                        StaggeredResultCard(result = result, index = index, delayMs = index * 80L)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun StaggeredResultCard(result: FallacyResult, index: Int, delayMs: Long) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInHorizontally(tween(400, easing = EaseOutCubic)) { it / 3 }
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
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
                    Spacer(Modifier.width(8.dp))
                    Text(result.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f))
                }

                if (result.quotedText.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = HighlightYellow,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "\"${result.quotedText}\"",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = HighlightText
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(result.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
            }
        }
    }
}

@Composable
private fun ReferenceGuidePanel(
    references: List<FallacyReference>,
    selected: FallacyReference?,
    onSelect: (FallacyReference?) -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Fallacy Reference Guide",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
        }
        Text("${references.size} common logical fallacies",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            references.forEachIndexed { index, ref ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 30L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 2 }
                ) {
                    Card(
                        onClick = { onSelect(if (selected == ref) null else ref) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected == ref)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(6.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(ref.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                                Icon(
                                    if (selected == ref) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            AnimatedVisibility(
                                visible = selected == ref,
                                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text(ref.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Example: \"${ref.example}\"",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
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
