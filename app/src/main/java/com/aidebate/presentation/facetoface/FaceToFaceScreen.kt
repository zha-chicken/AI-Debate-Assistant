@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.facetoface

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.domain.model.StructuredPhase
import com.aidebate.presentation.common.toDebateRole
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*

@Composable
fun FaceToFaceScreen(
    sessionId: String,
    onViewResult: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: FaceToFaceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.topicTitle.ifBlank { t.faceToFaceLabel },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, t.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isInitializing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                // Progress section
                TurnProgressHeader(
                    turnIndex = uiState.turnIndex,
                    currentPhase = uiState.currentPhase,
                    isComplete = uiState.isComplete
                )

                val result = uiState.result
                if (uiState.isComplete) {
                    // All turns submitted — show judge/results
                    if (result != null) {
                        CompleteWithResult(
                            result = result,
                            topicTitle = uiState.topicTitle,
                            onViewResult = { onViewResult(sessionId) }
                        )
                    } else if (uiState.isJudging) {
                        JudgingIndicator()
                    } else {
                        JudgePrompt(
                            onJudge = { viewModel.requestJudgment() }
                        )
                    }
                } else {
                    // Current turn — show player indicator + input
                    PlayerTurnCard(
                        isPlayer1 = uiState.isPlayer1Turn,
                        currentPhase = uiState.currentPhase,
                        turnCount = uiState.turnCount
                    )

                    // Input section
                    OutlinedTextField(
                        value = uiState.userInput,
                        onValueChange = { viewModel.onInputChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        placeholder = { Text(t.writeArgument) },
                        shape = Radii.mediumShape,
                        enabled = !uiState.isSaving,
                        minLines = 4,
                        maxLines = 8
                    )

                    // Submit button
                    Button(
                        onClick = { viewModel.submitTurn() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = uiState.userInput.isNotBlank() && !uiState.isSaving,
                        shape = Radii.mediumShape
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(t.send, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Error
                if (uiState.error != null) {
                    ErrorCard(
                        message = uiState.error!!,
                        onDismiss = { viewModel.clearError() }
                    )
                }
            }
        }
    }

    // Pass overlay (full screen, transparent)
    if (uiState.showPassOverlay) {
        PassOverlay(
            playerLabel = if (uiState.isPlayer1Turn) t.player2 else t.player1,
            onDismiss = { viewModel.dismissPassOverlay() }
        )
    }
}

// ============================================================
// TURN PROGRESS HEADER
// ============================================================

@Composable
private fun TurnProgressHeader(
    turnIndex: Int,
    currentPhase: StructuredPhase,
    isComplete: Boolean
) {
    val t = LocalTranslation.current
    val phaseLabel = when (currentPhase) {
        StructuredPhase.OPENING -> t.phaseOpening
        StructuredPhase.REBUTTAL -> t.phaseRebuttal
        StructuredPhase.CLOSING -> t.phaseClosing
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Phase label
        Text(
            phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(Spacing.sm))

        // Progress dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 6) {
                val isActive = if (!isComplete) i == turnIndex else i < turnIndex
                val isDone = i < turnIndex

                Box(
                    modifier = Modifier
                        .size(if (isActive) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> MaterialTheme.colorScheme.primary
                                isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )

                if (i < 5) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(
                                if (i < turnIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        // Turn counter
        if (!isComplete) {
            val turnCount = turnIndex + 1
            Text(
                "Turn $turnCount / 6",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

// ============================================================
// PLAYER TURN CARD
// ============================================================

@Composable
private fun PlayerTurnCard(
    isPlayer1: Boolean,
    currentPhase: StructuredPhase,
    turnCount: Int
) {
    val t = LocalTranslation.current
    val role = if (isPlayer1) DebateRole.PRO else DebateRole.CON
    val tokens = RoleTokenDefaults.forRole(role)

    val label = if (isPlayer1) t.player1Turn else t.player2Turn
    val playerName = if (isPlayer1) t.player1 else t.player2
    val sideLabel = if (isPlayer1) t.argueFor else t.argueAgainst

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(turnCount) {
        visible = false
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + scaleIn(tween(300))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Radii.largeShape,
            colors = CardDefaults.cardColors(
                containerColor = tokens.color.container.copy(alpha = 0.3f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(tokens.color.primary.copy(alpha = 0.3f))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Player icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(tokens.color.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = tokens.color.primary
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.color.primary
                )

                Spacer(Modifier.height(Spacing.xs))

                Text(
                    "$playerName · $sideLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.color.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ============================================================
// PASS OVERLAY
// ============================================================

@Composable
private fun PassOverlay(
    playerLabel: String,
    onDismiss: () -> Unit
) {
    val t = LocalTranslation.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = onDismiss,
            modifier = Modifier
                .padding(Spacing.xl)
                .fillMaxWidth(),
            shape = Radii.largeShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    String.format(t.passToPlayer, playerLabel),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(Spacing.sm))

                Text(
                    "Tap to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ============================================================
// JUDGE PROMPT
// ============================================================

@Composable
private fun JudgePrompt(onJudge: () -> Unit) {
    val t = LocalTranslation.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.largeShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(Spacing.md))

            Text(
                t.f2fComplete,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(Spacing.sm))

            Text(
                t.wouldYouLikeJudge,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(Spacing.md))

            Button(
                onClick = onJudge,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = Radii.mediumShape
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(t.judgeResults, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ============================================================
// JUDGING INDICATOR
// ============================================================

@Composable
private fun JudgingIndicator() {
    val t = LocalTranslation.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.largeShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(Spacing.md))
            Text(
                t.judgingInProgress,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ============================================================
// COMPLETE WITH RESULT
// ============================================================

@Composable
private fun CompleteWithResult(
    result: com.aidebate.domain.model.DebateResult,
    topicTitle: String,
    onViewResult: () -> Unit
) {
    val t = LocalTranslation.current

    val winnerRole = result.winner?.toDebateRole()
    val winnerTokens = winnerRole?.let { RoleTokenDefaults.forRole(it) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(400))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = Radii.largeShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary
                )

                Spacer(Modifier.height(Spacing.md))

                Text(
                    t.debateComplete,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (result.winner != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        String.format(t.winnerLabel, result.winner.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary
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

                OutlinedButton(
                    onClick = onViewResult,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radii.mediumShape
                ) {
                    Text(t.viewFullResult)
                }
            }
        }
    }
}

// ============================================================
// ERROR CARD
// ============================================================

@Composable
private fun ErrorCard(message: String, onDismiss: () -> Unit) {
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
            TextButton(onClick = onDismiss) { Text(t.dismiss) }
        }
    }
}
