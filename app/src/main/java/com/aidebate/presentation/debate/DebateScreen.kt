@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.debate

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.*

@Composable
fun DebateScreen(
    sessionId: String,
    onViewResult: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    LaunchedEffect(uiState.turns.size) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size - 1)
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
                        // Turn list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.turns.size) { index ->
                                val turn = uiState.turns[index]
                                AnimatedDebateBubble(turn = turn, index = index)
                            }

                            // Typing indicator
                            if (uiState.isThinking) {
                                item { TypingIndicator() }
                            }

                            // Tap to advance (AI vs AI)
                            val isWaitingForTap = uiState.contextualState is DebateContextualState.WaitingForTap
                            if (isWaitingForTap) {
                                item {
                                    val tapState = uiState.contextualState as DebateContextualState.WaitingForTap
                                    TapToAdvanceOverlay(
                                        nextSpeaker = tapState.nextSpeaker,
                                        nextProvider = tapState.nextProvider.displayName,
                                        onTap = { viewModel.onTapToAdvance() }
                                    )
                                }
                            }

                            // Completed state
                            if (uiState.contextualState is DebateContextualState.DebateCompleted ||
                                uiState.contextualState is DebateContextualState.Judging
                            ) {
                                item {
                                    DebateEndCard(
                                        result = uiState.result,
                                        isJudging = uiState.contextualState is DebateContextualState.Judging,
                                        onJudge = { viewModel.requestJudgment() },
                                        onViewResult = onViewResult
                                    )
                                }
                            }

                            // Error state
                            if (uiState.error != null) {
                                item {
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
                }
            }
        }
    }
}

@Composable
fun AnimatedDebateBubble(turn: DebateTurn, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(turn.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            tween(400, easing = EaseOutCubic),
            initialOffsetY = { it / 2 }
        )
    ) {
        DebateBubble(turn = turn)
    }
}

@Composable
fun DebateBubble(turn: DebateTurn) {
    val isProposition = turn.speakerRole == SpeakerRole.AI_PROPOSITION
    val isUser = turn.speakerRole == SpeakerRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when (turn.speakerRole) {
        SpeakerRole.AI_PROPOSITION -> MaterialTheme.colorScheme.primaryContainer
        SpeakerRole.AI_OPPOSITION -> MaterialTheme.colorScheme.secondaryContainer
        SpeakerRole.USER -> MaterialTheme.colorScheme.tertiaryContainer
        SpeakerRole.MODERATOR -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (turn.speakerRole) {
        SpeakerRole.AI_PROPOSITION -> MaterialTheme.colorScheme.onPrimaryContainer
        SpeakerRole.AI_OPPOSITION -> MaterialTheme.colorScheme.onSecondaryContainer
        SpeakerRole.USER -> MaterialTheme.colorScheme.onTertiaryContainer
        SpeakerRole.MODERATOR -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (turn.speakerRole) {
        SpeakerRole.AI_PROPOSITION -> "PRO"
        SpeakerRole.AI_OPPOSITION -> "CON"
        SpeakerRole.USER -> "YOU"
        SpeakerRole.MODERATOR -> "JUDGE"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(bubbleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            } else {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(bubbleColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bubbleColor
        ) {
            Text(
                text = turn.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }

        if (turn.providerUsed != null) {
            Text(
                "${turn.providerUsed.displayName}${turn.modelUsed?.let { " / $it" } ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dots = listOf(
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500)), label = "d1"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 150)), label = "d2"),
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(500, 300)), label = "d3")
    )

    Surface(
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.widthIn(max = 72.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (dot in dots) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(dot.value)
                        .alpha(dot.value)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(Modifier.width(4.dp))
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun TapToAdvanceOverlay(
    nextSpeaker: SpeakerRole,
    nextProvider: String,
    onTap: () -> Unit
) {
    val alpha = rememberInfiniteTransition(label = "tapPulse").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1000)), label = "alpha"
    )

    Card(
        onClick = onTap,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha.value)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Tap to see $nextProvider's response",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
fun DebateEndCard(
    result: DebateResult?,
    isJudging: Boolean,
    onJudge: () -> Unit,
    onViewResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result != null) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (result != null) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (result != null) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            if (result != null) {
                Text(
                    "Debate Complete",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (result.winner != null) {
                    Text(
                        "Winner: ${result.winner.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (result.summary.isNotBlank()) {
                        Text(
                            result.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onViewResult) {
                    Text("View Full Result")
                }
            } else if (isJudging) {
                Text("Judging in progress...", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Debate Complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Would you like an AI to judge this debate?",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Error", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(message, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

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
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
            Spacer(Modifier.width(8.dp))
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
