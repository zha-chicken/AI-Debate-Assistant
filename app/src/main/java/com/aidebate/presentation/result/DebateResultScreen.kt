@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.result

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.DebateTurn
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.DebateSession
import com.aidebate.domain.model.RecommendationFeedback
import com.aidebate.domain.model.RecommendationFeedbackReasonType
import com.aidebate.domain.model.RecommendationFeedbackSentiment
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.presentation.common.AiGeneratedDisclaimer
import com.aidebate.presentation.common.GlowWrapper
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*

@Composable
fun DebateResultScreen(
    sessionId: String,
    onBackToHome: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebateResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val t = LocalTranslation.current

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.debateResult, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val resultText = buildString {
                                append(String.format(t.resultTopic, uiState.topicTitle) + "\n\n")
                                uiState.turns.forEach { turn ->
                                    append("${turn.speakerRole.name}: ${turn.content}\n\n")
                                }
                                if (uiState.result != null) {
                                    val displayWinner = resolveDisplayWinner(
                                        winner = uiState.result!!.winner,
                                        summary = uiState.result!!.summary,
                                        session = uiState.session
                                    )
                                    append(String.format(t.resultWinner, formatWinner(displayWinner, uiState.session)) + "\n")
                                    append(uiState.result!!.summary)
                                }
                            }
                            val shareText = String.format(t.shareResult, resultText)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, t.share))
                        }) {
                            Icon(Icons.Default.Share, t.share)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                // Winner card
                item(key = "winner") {
                    WinnerCard(
                        topicTitle = uiState.topicTitle,
                        winner = uiState.result?.winner,
                        session = uiState.session,
                        summary = uiState.result?.summary ?: ""
                    )
                }

                if (uiState.session?.mode != DebateMode.AI_VS_AI && uiState.result != null) {
                    item(key = "recommendationFeedback") {
                        ResultFeedbackSection(
                            feedback = uiState.recommendationFeedback,
                            onRecord = viewModel::recordRecommendationFeedback
                        )
                    }
                }

                // Timeline visualization
                if (uiState.turns.isNotEmpty()) {
                    item(key = "timeline") {
                        TimelineSection(turns = uiState.turns)
                    }
                }

                // Transcript
                item(key = "transcriptHeader") {
                    Text(
                        t.transcript,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }

                items(uiState.turns, key = { it.id }) { turn ->
                    TurnCard(turn = turn)
                }

                // Back to home
                item(key = "backHome") {
                    Spacer(Modifier.height(Spacing.sm))
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = Radii.mediumShape,
                        colors = glassButtonColors()
                    ) {
                        Icon(Icons.Default.Home, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(t.backToHome)
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ResultFeedbackSection(
    feedback: RecommendationFeedback?,
    onRecord: (RecommendationFeedbackSentiment, RecommendationFeedbackReasonType) -> Unit
) {
    val t = LocalTranslation.current
    var pendingSentiment by remember { mutableStateOf<RecommendationFeedbackSentiment?>(null) }

    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                t.recommendationFeedbackTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                feedback?.let {
                    "${t.saved}: ${if (it.sentiment == RecommendationFeedbackSentiment.LIKE) t.like else t.dislike} · ${if (it.reasonType == RecommendationFeedbackReasonType.CATEGORY) t.category else t.technique}"
                } ?: t.recommendationFeedbackSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FeedbackButton(
                    title = t.like,
                    icon = Icons.Filled.ThumbUp,
                    selected = feedback?.sentiment == RecommendationFeedbackSentiment.LIKE,
                    onClick = { pendingSentiment = RecommendationFeedbackSentiment.LIKE },
                    modifier = Modifier.weight(1f)
                )
                FeedbackButton(
                    title = t.dislike,
                    icon = Icons.Filled.ThumbDown,
                    selected = feedback?.sentiment == RecommendationFeedbackSentiment.DISLIKE,
                    onClick = { pendingSentiment = RecommendationFeedbackSentiment.DISLIKE },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    pendingSentiment?.let { sentiment ->
        AlertDialog(
            onDismissRequest = { pendingSentiment = null },
            title = {
                Text(if (sentiment == RecommendationFeedbackSentiment.LIKE) t.whatDoYouLike else t.whatDoYouDislike)
            },
            text = { Text(t.recommendationFeedbackSubtitle) },
            confirmButton = {
                TextButton(onClick = {
                    onRecord(sentiment, RecommendationFeedbackReasonType.CATEGORY)
                    pendingSentiment = null
                }) {
                    Text(t.category)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        onRecord(sentiment, RecommendationFeedbackReasonType.TECHNIQUE)
                        pendingSentiment = null
                    }) {
                        Text(t.technique)
                    }
                    TextButton(onClick = { pendingSentiment = null }) {
                        Text(t.cancel)
                    }
                }
            }
        )
    }
}

@Composable
private fun FeedbackButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (selected) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = Radii.mediumShape
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
        Spacer(Modifier.width(Spacing.xs))
        Text(title, color = accent)
    }
}

@Composable
private fun WinnerCard(
    topicTitle: String,
    winner: SpeakerRole?,
    session: DebateSession?,
    summary: String,
) {
    val t = LocalTranslation.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val displayWinner = resolveDisplayWinner(winner, summary, session)
    val winnerTokens = displayWinner?.toDebateRole()?.let { RoleTokenDefaults.forRole(it) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(400))
    ) {
        GlowWrapper(
            glowColor = winnerTokens?.color?.glow ?: MaterialTheme.colorScheme.tertiary,
            shape = Radii.largeShape,
            isActive = true,
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), accent = winnerTokens?.color?.primary ?: WarmGlow) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(Spacing.sm))

                    Icon(
                        Icons.Default.EmojiEvents, null,
                        Modifier.size(56.dp),
                        tint = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(Spacing.md))

                    Text(
                        topicTitle,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = RhetorixTextColors.Primary
                    )
                    Spacer(Modifier.height(Spacing.sm))

                    if (displayWinner != null) {
                        Text(
                            t.winner,
                            style = MaterialTheme.typography.labelLarge,
                            color = RhetorixTextColors.Secondary
                        )
                        Text(
                            formatWinner(displayWinner, session),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = winnerTokens?.color?.primary ?: MaterialTheme.colorScheme.tertiary
                        )
                        if (summary.isNotBlank()) {
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                summary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = RhetorixTextColors.Secondary
                            )
                            AiGeneratedDisclaimer(
                                modifier = Modifier.padding(top = 4.dp),
                                color = RhetorixTextColors.Secondary.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Text(
                            t.noWinnerDeclared,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun formatWinner(winner: SpeakerRole?, session: DebateSession?): String {
    return when (winner) {
        SpeakerRole.USER -> "You"
        SpeakerRole.AI_PROPOSITION -> {
            if (session?.mode == DebateMode.USER_VS_AI) "AI - Support" else "Support"
        }
        SpeakerRole.AI_OPPOSITION -> {
            if (session?.mode == DebateMode.USER_VS_AI) "AI - Oppose" else "Oppose"
        }
        SpeakerRole.MODERATOR -> "Judge"
        null -> "N/A"
    }
}

private fun resolveDisplayWinner(
    winner: SpeakerRole?,
    summary: String,
    session: DebateSession?
): SpeakerRole? {
    if (session?.mode != DebateMode.USER_VS_AI) return winner
    val text = summary.lowercase()
    val aiSide = if (session.userSide == SpeakerRole.AI_PROPOSITION) {
        SpeakerRole.AI_OPPOSITION
    } else {
        SpeakerRole.AI_PROPOSITION
    }
    val aiWinSignals = listOf(
        "ai won",
        "ai wins",
        "ai presented",
        "ai's argument",
        "ai’s argument",
        "uncontested",
        "user provided no",
        "user offered no",
        "without any rebuttal from the user",
        "without rebuttal from the user",
        "user failed"
    )
    val userWinSignals = listOf(
        "user won",
        "user wins",
        "user presented a stronger",
        "user provided a stronger",
        "user's argument was stronger",
        "user’s argument was stronger",
        "ai provided no",
        "ai failed"
    )
    val aiWins = aiWinSignals.any { it in text }
    val userWins = userWinSignals.any { it in text }
    return when {
        aiWins && !userWins -> aiSide
        userWins && !aiWins -> SpeakerRole.USER
        else -> winner
    }
}

@Composable
private fun TimelineSection(turns: List<DebateTurn>) {
    val t = LocalTranslation.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, delayMillis = 200))
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    t.keyMoments,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )

                // Simple timeline visualization
                turns.filterIndexed { index, _ -> index % 2 == 0 || index == turns.lastIndex }
                    .take(5)
                    .forEachIndexed { i, turn ->
                        val role = turn.speakerRole.toDebateRole()
                        val tokens = RoleTokenDefaults.forRole(role)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timeline dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(tokens.color.primary.copy(alpha = 0.6f))
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Text(
                                tokens.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tokens.color.primary,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(
                                turn.content.take(60) + if (turn.content.length > 60) t.ellipsis else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (i < minOf(turns.size - 1, 4)) {
                            // Vertical connector line
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(12.dp)
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun TurnCard(turn: DebateTurn) {
    val role = turn.speakerRole.toDebateRole()
    val tokens = RoleTokenDefaults.forRole(role)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(turn.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), accent = tokens.color.primary) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(tokens.color.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tokens.label.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.color.primary
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        tokens.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.color.primary
                    )
                    if (turn.phase != null) {
                        Spacer(Modifier.width(Spacing.sm))
                        Text(
                            turn.phase.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(turn.content, style = MaterialTheme.typography.bodyMedium)
                if (turn.speakerRole != SpeakerRole.USER) {
                    AiGeneratedDisclaimer(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                    )
                }
            }
        }
    }
}

private fun SpeakerRole.toDebateRole() = when (this) {
    SpeakerRole.AI_PROPOSITION -> DebateRole.PRO
    SpeakerRole.AI_OPPOSITION -> DebateRole.CON
    SpeakerRole.USER -> DebateRole.USER
    SpeakerRole.MODERATOR -> DebateRole.MODERATOR
}
