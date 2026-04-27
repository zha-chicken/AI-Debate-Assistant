@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.debate

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.*
import com.aidebate.presentation.common.*
import com.aidebate.presentation.theme.*

// ============================================================
// TIMELINE — computed to interleave turns and phase dividers
// ============================================================

private sealed interface TimelineItem {
    data class Turn(val turn: DebateTurn) : TimelineItem
    data class Phase(val phase: StructuredPhase) : TimelineItem
}

// ============================================================
// SCREEN
// ============================================================

@Composable
fun DebateScreen(
    sessionId: String,
    onViewResult: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isAiVsAi = uiState.mode == DebateMode.AI_VS_AI

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    // Auto-scroll to latest item
    LaunchedEffect(uiState.turns.size) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size - 1)
        }
    }

    // Build timeline with phase dividers
    val timeline = remember(uiState.turns) {
        buildList {
            var lastPhase: StructuredPhase? = null
            for (turn in uiState.turns) {
                if (turn.phase != null && turn.phase != lastPhase) {
                    add(TimelineItem.Phase(turn.phase))
                    lastPhase = turn.phase
                }
                add(TimelineItem.Turn(turn))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.topicTitle.ifBlank { "Debate" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (uiState.currentPhase != null && uiState.format == DebateFormat.STRUCTURED) {
                            Text(
                                uiState.currentPhase!!.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.turns.isNotEmpty()) viewModel.endDebate()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val isUserTurn = uiState.contextualState is DebateContextualState.WaitingForUserInput
            if (isUserTurn && uiState.mode == DebateMode.USER_VS_AI) {
                UserInputBar(
                    value = uiState.userInputText,
                    onValueChange = { viewModel.onUserInputChanged(it) },
                    onSend = { viewModel.submitUserTurn() },
                    enabled = !uiState.isThinking
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isInitializing -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            // Turns with phase dividers
                            items(timeline.size, key = { timeline[it].hashCode() }) { index ->
                                when (val item = timeline[index]) {
                                    is TimelineItem.Turn -> {
                                        ConversationUnit(
                                            turn = item.turn,
                                            importance = TurnImportance.NORMAL
                                        )
                                    }
                                    is TimelineItem.Phase -> {
                                        val label = when (item.phase) {
                                            StructuredPhase.OPENING -> "Opening Arguments"
                                            StructuredPhase.REBUTTAL -> "Rebuttal Phase"
                                            StructuredPhase.CLOSING -> "Closing Arguments"
                                        }
                                        PhaseDivider(label = label)
                                    }
                                }
                            }

                            // Typing indicator
                            if (uiState.isThinking) {
                                item(key = "typing") {
                                    val thinkingRole = when (val state = uiState.contextualState) {
                                        is DebateContextualState.WaitingForAiTurn -> state.speakerRole.toDebateRole()
                                        else -> DebateRole.PRO
                                    }
                                    val tokens = RoleTokenDefaults.forRole(thinkingRole)
                                    SmartTypingIndicator(
                                        roleName = tokens.label,
                                        roleTokens = tokens
                                    )
                                }
                            }

                            // Tap to advance (AI vs AI)
                            val isWaitingForTap = uiState.contextualState is DebateContextualState.WaitingForTap
                            if (isWaitingForTap) {
                                item(key = "tapAdvance") {
                                    val tapState = uiState.contextualState as DebateContextualState.WaitingForTap
                                    TapToAdvanceOverlay(
                                        nextSpeaker = tapState.nextSpeaker,
                                        nextProvider = tapState.nextProvider.displayName,
                                        onTap = { viewModel.onTapToAdvance() }
                                    )
                                }
                            }

                            // End card
                            if (uiState.contextualState is DebateContextualState.DebateCompleted ||
                                uiState.contextualState is DebateContextualState.Judging
                            ) {
                                item(key = "endCard") {
                                    CelebrationEndCard(
                                        result = uiState.result,
                                        isJudging = uiState.contextualState is DebateContextualState.Judging,
                                        onJudge = { viewModel.requestJudgment() },
                                        onViewResult = onViewResult
                                    )
                                }
                            }

                            // Error
                            if (uiState.error != null) {
                                item(key = "error") {
                                    ErrorCard(
                                        message = uiState.error!!,
                                        onDismiss = { viewModel.clearError() },
                                        onRetry = {
                                            when (uiState.contextualState) {
                                                is DebateContextualState.WaitingForTap -> viewModel.onTapToAdvance()
                                                else -> viewModel.clearError()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // AI vs AI split-color background
                    if (isAiVsAi && uiState.turns.isNotEmpty()) {
                        AiVsAiBackground()
                    }
                }
            }
        }
    }
}

// ============================================================
// AI vs AI — split-color background overlay
// ============================================================

@Composable
private fun AiVsAiBackground() {
    val proTokens = RoleTokenDefaults.Pro
    val conTokens = RoleTokenDefaults.Con

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    proTokens.color.dim.copy(alpha = 0.03f),
                    Color.Transparent,
                    conTokens.color.dim.copy(alpha = 0.03f),
                )
            )
        )
    }
}

// ============================================================
// TAP TO ADVANCE
// ============================================================

@Composable
fun TapToAdvanceOverlay(
    nextSpeaker: SpeakerRole,
    nextProvider: String,
    onTap: () -> Unit
) {
    val role = nextSpeaker.toDebateRole()
    val tokens = RoleTokenDefaults.forRole(role)
    val alpha = rememberInfiniteTransition(label = "tapPulse").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1000)), label = "alpha"
    )

    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = tokens.color.container.copy(alpha = alpha.value * 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                tint = tokens.color.primary
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                "Tap to see $nextProvider's response",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tokens.color.primary
            )
        }
    }
}

// ============================================================
// CELEBRATION END CARD
// ============================================================

@Composable
fun CelebrationEndCard(
    result: DebateResult?,
    isJudging: Boolean,
    onJudge: () -> Unit,
    onViewResult: () -> Unit
) {
    val winnerRole = result?.winner?.toDebateRole()
    val winnerTokens = winnerRole?.let { RoleTokenDefaults.forRole(it) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(400)) { it / 2 }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Radii.largeShape,
            colors = CardDefaults.cardColors(
                containerColor = if (result != null) MaterialTheme.colorScheme.tertiaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (result != null) {
                    // Trophy with glow
                    GlowWrapper(
                        glowColor = winnerTokens?.color?.glow ?: MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape,
                        isActive = true,
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))

                    // Animated title
                    Text(
                        "Debate Complete",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (result.winner != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "Winner: ${result.winner.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    if (result.summary.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            result.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    OutlinedButton(onClick = onViewResult) {
                        Text("View Full Result")
                    }
                } else if (isJudging) {
                    Text("Judging in progress...", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.sm))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text("Debate Complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Would you like an AI to judge this debate?",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.xs))
                    Spacer(Modifier.height(Spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        OutlinedButton(onClick = onViewResult) { Text("Skip") }
                        Button(onClick = onJudge) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Judge Debate")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// ERROR CARD
// ============================================================

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(Spacing.sm))
                Text("Error", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = Spacing.xs))
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

// ============================================================
// USER INPUT BAR
// ============================================================

@Composable
fun UserInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write your argument...") },
                shape = RoundedCornerShape(24.dp),
                minLines = 1,
                maxLines = 4,
                enabled = enabled
            )
            Spacer(Modifier.width(Spacing.sm))
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(48.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Send, "Send")
            }
        }
    }
}

