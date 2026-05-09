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
import com.aidebate.presentation.localization.LocalTranslation
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
    val t = LocalTranslation.current
    val listState = rememberLazyListState()
    val isAiVsAi = uiState.mode == DebateMode.AI_VS_AI

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    uiState.safetyWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSafetyWarning() },
            icon = { Icon(Icons.Default.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("内容已拦截", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(warning, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSafetyWarning() }) {
                    Text(t.dismiss)
                }
            },
            containerColor = GlassSurfaceStrong,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val isWaitingForTap = uiState.contextualState is DebateContextualState.WaitingForTap
    val isEndState = uiState.contextualState is DebateContextualState.DebateCompleted ||
        uiState.contextualState is DebateContextualState.Judging
    val listItemCount = 1 + timeline.size +
        if (uiState.isThinking) 1 else 0 +
        if (isWaitingForTap) 1 else 0 +
        if (isEndState) 1 else 0 +
        if (uiState.error != null) 1 else 0

    // Auto-scroll to the newest meaningful item. The old turn-count index pointed
    // near the top because the list also contains status and phase divider items.
    LaunchedEffect(listItemCount) {
        if (listItemCount > 1) {
            listState.animateScrollToItem(listItemCount - 1)
        }
    }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                uiState.topicTitle.ifBlank { t.debateTitle },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.currentPhase != null && uiState.format == DebateFormat.STRUCTURED) {
                                Text(
                                    uiState.currentPhase!!.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryLight
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.turns.isNotEmpty()) viewModel.endDebate()
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            }
            ,
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
                            item(key = "status") {
                                DebateStatusPanel(
                                    uiState = uiState,
                                    onEndEarly = { viewModel.endDebateAndJudge() }
                                )
                            }

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
                                            StructuredPhase.OPENING -> t.phaseOpening
                                            StructuredPhase.REBUTTAL -> t.phaseRebuttal
                                            StructuredPhase.CLOSING -> t.phaseClosing
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
}

@Composable
private fun DebateStatusPanel(
    uiState: DebateUiState,
    onEndEarly: () -> Unit,
) {
    val t = LocalTranslation.current
    val supportTurns = uiState.turns.count { it.speakerRole == SpeakerRole.AI_PROPOSITION || it.speakerRole == SpeakerRole.USER }
    val opposeTurns = uiState.turns.count { it.speakerRole == SpeakerRole.AI_OPPOSITION }
    val targetTurns = when {
        uiState.format == DebateFormat.STRUCTURED && uiState.mode == DebateMode.USER_VS_AI -> 12
        uiState.format == DebateFormat.STRUCTURED && uiState.mode == DebateMode.AI_VS_AI -> 6
        else -> null
    }
    val turnLabel = targetTurns?.let { "${uiState.turns.size.coerceAtMost(it)} / $it" }
        ?: uiState.turns.size.toString()
    val canEndEarly = uiState.mode == DebateMode.USER_VS_AI &&
        uiState.turns.isNotEmpty() &&
        uiState.contextualState !is DebateContextualState.DebateCompleted &&
        uiState.contextualState !is DebateContextualState.Judging &&
        !uiState.isThinking
    val roundLabel = uiState.currentPhase?.let {
        when (it) {
            StructuredPhase.OPENING -> t.phaseOpening
            StructuredPhase.REBUTTAL -> t.phaseRebuttal
            StructuredPhase.CLOSING -> t.phaseClosing
        }
    } ?: if (uiState.format == DebateFormat.FREE_FLOW) t.freeFlow else t.debateTitle

    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$supportTurns",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryLight,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(t.argueFor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                Spacer(Modifier.weight(1f))
                Surface(shape = Radii.smallShape, color = GlassSurfaceStrong) {
                    Text(
                        roundLabel,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(t.argueAgainst, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "$opposeTurns",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = {
                    val maxTurns = targetTurns ?: 12
                    uiState.turns.size.coerceAtMost(maxTurns).toFloat() / maxTurns.toFloat()
                },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(999.dp)),
                color = Secondary,
                trackColor = Primary.copy(alpha = 0.28f)
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "Turn $turnLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                modifier = Modifier.align(Alignment.End)
            )
            if (canEndEarly) {
                Spacer(Modifier.height(Spacing.sm))
                OutlinedButton(
                    onClick = onEndEarly,
                    modifier = Modifier.align(Alignment.End),
                    shape = Radii.smallShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WarmGlow
                    )
                ) {
                    Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Spacing.xs))
                    Text(t.endAndJudge)
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
    val t = LocalTranslation.current
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
                String.format(t.tapToSeeResponse, nextProvider),
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
    val t = LocalTranslation.current
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
                        t.debateComplete,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (result.winner != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = String.format(t.winnerLabel, result.winner.name),
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
                        AiGeneratedDisclaimer(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.56f)
                        )
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    OutlinedButton(onClick = onViewResult) {
                        Text(t.viewFullResult)
                    }
                } else if (isJudging) {
                    Text(t.judgingInProgress, style = MaterialTheme.typography.titleMedium)
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
                    Text(t.debateComplete, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(t.wouldYouLikeJudge,
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Spacing.xs))
                    Spacer(Modifier.height(Spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        OutlinedButton(onClick = onViewResult) { Text(t.skip) }
                        Button(onClick = onJudge) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t.judgeDebate)
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
    val t = LocalTranslation.current
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
                Text(t.errorGeneric, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = Spacing.xs))
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                TextButton(onClick = onDismiss) { Text(t.dismiss) }
                Button(onClick = onRetry) { Text(t.retry) }
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
    val t = LocalTranslation.current
    Surface(
        color = GlassSurfaceStrong,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
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
                placeholder = { Text(t.writeArgument) },
                shape = RoundedCornerShape(24.dp),
                minLines = 1,
                maxLines = 4,
                enabled = enabled,
                colors = glassTextFieldColors()
            )
            Spacer(Modifier.width(Spacing.sm))
            FilledIconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary.copy(alpha = 0.72f),
                    contentColor = OnSurfaceDark
                )
            ) {
                Icon(Icons.Default.Send, t.send)
            }
        }
    }
}
