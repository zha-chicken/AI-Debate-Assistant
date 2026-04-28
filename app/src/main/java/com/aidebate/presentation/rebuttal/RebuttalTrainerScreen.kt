@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.rebuttal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.common.RolePill
import com.aidebate.presentation.theme.*
import com.aidebate.domain.model.RebuttalChatMessage
import com.aidebate.domain.model.RebuttalExplanation
import com.aidebate.domain.model.ScoreBreakdown

@Composable
fun RebuttalTrainerScreen(
    sessionId: String = "",
    onBack: () -> Unit,
    viewModel: RebuttalTrainerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        if (sessionId.isNotBlank()) viewModel.loadSession(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rebuttal Trainer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.phase != TrainerPhase.TOPIC_SELECT) viewModel.backToTopics()
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                TrainerPhase.TOPIC_SELECT -> TopicSelectPhase(uiState, viewModel)
                TrainerPhase.SETUP -> SetupPhase(uiState, viewModel)
                TrainerPhase.READY -> ReadyPhase(uiState, viewModel)
                TrainerPhase.RESPONDING -> RespondingPhase(uiState, viewModel)
                TrainerPhase.SCORING -> ScoringPhase()
                TrainerPhase.RESULT -> ResultPhase(uiState, viewModel)
            }

            if (uiState.error != null) {
                ErrorBanner(uiState.error!!) { viewModel.clearError() }
            }
        }
    }
}

// ============================================================
// TOPIC SELECT
// ============================================================

@Composable
private fun TopicSelectPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg)) {
        Text("Select a Topic", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.sm))
        Text("Choose a debate topic to practice your rebuttal skills.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(Spacing.lg))

        uiState.topics.forEach { topic ->
            Card(
                onClick = { viewModel.selectTopic(topic.id, topic.title) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = Radii.mediumShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Topic, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (topic.description.isNotBlank()) {
                            Text(topic.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                }
            }
        }
    }
}

// ============================================================
// SETUP
// ============================================================

@Composable
private fun SetupPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg)) {
        if (uiState.selectedTopicTitle.isNotBlank()) {
            Card(
                shape = Radii.mediumShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(Modifier.padding(Spacing.lg)) {
                    Icon(Icons.Filled.Topic, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Text(uiState.selectedTopicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        SectionHeader("Your Position")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RolePill(
                selected = uiState.userSide == "FOR",
                onClick = { viewModel.setSide("FOR") },
                role = DebateRole.PRO,
                label = "For"
            )
            RolePill(
                selected = uiState.userSide == "AGAINST",
                onClick = { viewModel.setSide("AGAINST") },
                role = DebateRole.CON,
                label = "Against"
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader("Difficulty")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf("easy", "medium", "hard").forEach { diff ->
                FilterChip(
                    selected = uiState.difficulty == diff,
                    onClick = { viewModel.setDifficulty(diff) },
                    label = { Text(diff.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader("Time Limit")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(30 to "30s", 60 to "60s", 90 to "90s").forEach { (sec, label) ->
                FilterChip(
                    selected = uiState.timeLimitSec == sec,
                    onClick = { viewModel.setTimeLimit(sec) },
                    label = { Text(label) },
                    leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { viewModel.startSession() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = Radii.mediumShape,
            enabled = uiState.selectedTopicId != null
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("Generating Argument...")
            } else {
                Text("Start Training")
            }
        }
    }
}

// ============================================================
// READY
// ============================================================

@Composable
private fun ReadyPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        Text("Your Argument", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Read and prepare. Start the timer when ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(Spacing.lg))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = Radii.largeShape
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                Text("THE ARGUMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                Spacer(Modifier.height(Spacing.sm))
                Text(uiState.promptArgument,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AssistChip(onClick = {}, label = { Text("${uiState.timeLimitSec}s") },
                leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) })
            AssistChip(onClick = {}, label = { Text(uiState.difficulty.replaceFirstChar { it.uppercase() }) },
                leadingIcon = { Icon(Icons.Filled.Speed, null, Modifier.size(16.dp)) })
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { viewModel.startTimer() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = Radii.mediumShape
        ) {
            Icon(Icons.Filled.Timer, null)
            Spacer(Modifier.width(Spacing.sm))
            Text("Start Timer & Write Rebuttal")
        }
    }
}

// ============================================================
// RESPONDING — with circular timer progress
// ============================================================

@Composable
private fun RespondingPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val urgency = uiState.timeRemainingSec <= 10
    val totalTime = uiState.timeLimitSec.toFloat().coerceAtLeast(1f)
    val progress = uiState.timeRemainingSec / totalTime
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val tensionAlpha by rememberInfiniteTransition(label = "tension").animateFloat(
        0f, 0.03f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tensionAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (urgency) Modifier.background(errorColor.copy(alpha = tensionAlpha))
                else Modifier
            )
    ) {
        Column(Modifier.fillMaxSize().padding(Spacing.lg)) {
            // Circular timer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (urgency) errorContainer else primaryContainer
                ),
                shape = Radii.largeShape
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(Spacing.lg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val c = if (urgency) errorColor else primaryColor
                            drawArc(color = c.copy(alpha = 0.2f), startAngle = -90f,
                                sweepAngle = 360f, useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(color = c, startAngle = -90f,
                                sweepAngle = 360f * progress, useCenter = false,
                                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        formatTimer(uiState.timeRemainingSec),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (urgency) errorColor else primaryColor
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Prompt preview
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceVariant),
                shape = Radii.smallShape
            ) {
                Text(uiState.promptArgument, Modifier.padding(Spacing.md),
                    style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }

            Spacer(Modifier.height(Spacing.md))

            // Response input
            OutlinedTextField(
                value = uiState.userResponse,
                onValueChange = { viewModel.onResponseChanged(it) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Write your rebuttal...") },
                placeholder = { Text("Type your counter-argument here...") },
                minLines = 5,
                shape = Radii.mediumShape
            )

            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = { viewModel.submitRebuttal() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = Radii.mediumShape,
                enabled = uiState.userResponse.isNotBlank()
            ) {
                Icon(Icons.Filled.Send, null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Submit Rebuttal")
            }
        }
    }
}

// ============================================================
// SCORING
// ============================================================

@Composable
private fun ScoringPhase() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(Modifier.size(32.dp))
        Spacer(Modifier.height(Spacing.lg))
        Text("Analyzing your rebuttal...", style = MaterialTheme.typography.bodyLarge)
    }
}

// ============================================================
// RESULT — with performance grade
// ============================================================

@Composable
private fun ResultPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val attempt = uiState.currentAttempt ?: return

    val displayScore by animateIntAsState(
        targetValue = attempt.totalScore,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "totalScore"
    )

    val grade = when {
        attempt.totalScore >= 90 -> "A"
        attempt.totalScore >= 75 -> "B"
        attempt.totalScore >= 50 -> "C"
        else -> "D"
    }
    val gradeColor = when (grade) {
        "A" -> SuccessGreen; "B" -> Tertiary; "C" -> WarningAmber
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())
    ) {
        Text("Score Card", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xl))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = Radii.largeShape
        ) {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TOTAL SCORE", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Spacer(Modifier.height(Spacing.sm))
                Text("$displayScore / 100",
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(Spacing.sm))
                Surface(shape = CircleShape, color = gradeColor.copy(alpha = 0.15f)) {
                    Text(grade, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = gradeColor)
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AnimatedScoreChip("Logic", attempt.logicScore, Modifier.weight(1f))
            AnimatedScoreChip("Clarity", attempt.clarityScore, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AnimatedScoreChip("Persuasion", attempt.persuasionScore, Modifier.weight(1f))
            AnimatedScoreChip("Evidence", attempt.evidenceScore, Modifier.weight(1f))
        }

        Spacer(Modifier.height(Spacing.xl))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = Radii.largeShape
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                Text("Feedback", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(Spacing.sm))
                Text(attempt.feedback, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        // ———— EXPLAIN SCORE SECTION ————

        if (uiState.explanation == null) {
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = { viewModel.requestExplanation() },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = Radii.mediumShape,
                enabled = !uiState.isExplaining
            ) {
                if (uiState.isExplaining) {
                    CircularProgressIndicator(Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Getting detailed breakdown...")
                } else {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Explain Score")
                }
            }
        } else {
            // Per-category breakdown
            Spacer(Modifier.height(Spacing.lg))
            Text("Detailed Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.sm))

            uiState.explanation!!.breakdown.forEach { breakdown ->
                BreakdownCard(breakdown)
                Spacer(Modifier.height(Spacing.sm))
            }

            // Overall advice
            if (uiState.explanation!!.overallAdvice.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = Radii.mediumShape
                ) {
                    Column(Modifier.padding(Spacing.lg)) {
                        Text("Overall Advice", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(uiState.explanation!!.overallAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            // Key takeaway
            if (uiState.explanation!!.keyTakeaway.isNotBlank()) {
                Card(
                    shape = Radii.mediumShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(uiState.explanation!!.keyTakeaway,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            // ———— CHAT SECTION ————
            Text("Ask About This Score", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Spacing.sm))

            uiState.chatMessages.forEach { msg ->
                ChatBubble(msg)
                Spacer(Modifier.height(Spacing.xs))
            }

            // Chat input
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = uiState.chatInput,
                    onValueChange = { viewModel.onChatInputChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your score...") },
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(Spacing.sm))
                FilledIconButton(
                    onClick = { viewModel.sendChatMessage() },
                    enabled = uiState.chatInput.isNotBlank(),
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Send, "Send")
                }
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Time info card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = Radii.mediumShape
        ) {
            Row(Modifier.fillMaxWidth().padding(Spacing.md), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Time taken", style = MaterialTheme.typography.labelSmall)
                    Text("${attempt.timeTakenMs / 1000}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Limit", style = MaterialTheme.typography.labelSmall)
                    Text("${attempt.timeLimitSec}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = { viewModel.newRound() }, modifier = Modifier.weight(1f)) {
                Text("New Round")
            }
            Button(onClick = { viewModel.startTimer() }, modifier = Modifier.weight(1f)) {
                Text("Retry Same")
            }
        }
    }
}

// ============================================================
// COMPONENTS
// ============================================================

@Composable
private fun AnimatedScoreChip(label: String, score: Int, modifier: Modifier = Modifier) {
    val displayScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(600, delayMillis = 400, easing = EaseOutCubic),
        label = "subScore"
    )
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = Radii.mediumShape
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$displayScore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = when { score >= 20 -> SuccessGreen; score >= 15 -> WarningAmber; else -> MaterialTheme.colorScheme.error })
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("/25", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(Spacing.sm))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

// ============================================================
// BREAKDOWN CARD — per-category explanation
// ============================================================

@Composable
private fun BreakdownCard(breakdown: ScoreBreakdown) {
    val color = when {
        breakdown.score >= 20 -> SuccessGreen
        breakdown.score >= 15 -> WarningAmber
        else -> MaterialTheme.colorScheme.error
    }
    Card(
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.06f)
        )
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(breakdown.category,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(Spacing.sm))
                Text("${breakdown.score}/25",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = color)
            }
            Spacer(Modifier.height(4.dp))
            if (breakdown.strength.isNotBlank()) {
                Text("✓ ${breakdown.strength}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            if (breakdown.weakness.isNotBlank()) {
                Text("△ ${breakdown.weakness}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            if (breakdown.suggestion.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = Radii.smallShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(breakdown.suggestion,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                }
            }
        }
    }
}

// ============================================================
// CHAT BUBBLE
// ============================================================

@Composable
private fun ChatBubble(msg: RebuttalChatMessage) {
    val isUser = msg.role == "user"
    val bgColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp,
                bottomStart = if (isUser) 12.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 12.dp
            ),
            color = bgColor
        ) {
            Text(msg.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins > 0) "$mins:${secs.toString().padStart(2, '0')}" else "${secs}s"
}
