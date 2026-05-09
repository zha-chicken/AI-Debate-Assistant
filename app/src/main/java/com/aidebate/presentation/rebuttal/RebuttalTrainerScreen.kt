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
import com.aidebate.presentation.common.AiGeneratedDisclaimer
import com.aidebate.presentation.common.RolePill
import com.aidebate.presentation.theme.*
import com.aidebate.domain.model.RebuttalChatMessage
import com.aidebate.domain.model.RebuttalExplanation
import com.aidebate.domain.model.ScoreBreakdown
import com.aidebate.presentation.localization.LocalTranslation

@Composable
fun RebuttalTrainerScreen(
    sessionId: String = "",
    onBack: () -> Unit,
    viewModel: RebuttalTrainerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    LaunchedEffect(sessionId) {
        if (sessionId.isNotBlank()) viewModel.loadSession(sessionId)
    }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.rebuttalTrainerTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.phase != TrainerPhase.TOPIC_SELECT) viewModel.backToTopics()
                            else onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
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
}

// ============================================================
// TOPIC SELECT
// ============================================================

@Composable
private fun TopicSelectPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val t = LocalTranslation.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg)) {
        Text(t.selectTopic, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.sm))
        Text(t.selectTopicSub,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(Spacing.lg))

        uiState.topics.forEach { topic ->
            GlassCard(
                onClick = { viewModel.selectTopic(topic.id, topic.title) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                accent = Primary
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
    val t = LocalTranslation.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg)) {
        if (uiState.selectedTopicTitle.isNotBlank()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
                Row(Modifier.padding(Spacing.lg)) {
                    Icon(Icons.Filled.Topic, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.md))
                    Text(uiState.selectedTopicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        SectionHeader(t.yourPosition)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            RolePill(
                selected = uiState.userSide == "FOR",
                onClick = { viewModel.setSide("FOR") },
                role = DebateRole.PRO,
                label = t.argueFor
            )
            RolePill(
                selected = uiState.userSide == "AGAINST",
                onClick = { viewModel.setSide("AGAINST") },
                role = DebateRole.CON,
                label = t.argueAgainst
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader(t.difficulty)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf("easy", "medium", "hard").forEach { diff ->
                FilterChip(
                    selected = uiState.difficulty == diff,
                    onClick = { viewModel.setDifficulty(diff) },
                    label = {
                        Text(when (diff) {
                            "easy" -> t.easy; "medium" -> t.medium; "hard" -> t.hard
                            else -> diff.replaceFirstChar { it.uppercase() }
                        })
                    }
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        SectionHeader(t.timeLimit)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(30, 60, 90).forEach { sec ->
                FilterChip(
                    selected = uiState.timeLimitSec == sec,
                    onClick = { viewModel.setTimeLimit(sec) },
                    label = { Text(String.format(t.timeSec, sec)) },
                    leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { viewModel.startSession() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = Radii.mediumShape,
            enabled = uiState.selectedTopicId != null,
            colors = glassButtonColors()
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(t.generatingArgument)
            } else {
                Text(t.startTraining)
            }
        }
    }
}

// ============================================================
// READY
// ============================================================

@Composable
private fun ReadyPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val t = LocalTranslation.current
    Column(Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())) {
        Text(t.yourArgument, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(t.readyInstruction,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(Spacing.lg))

        GlassCard(modifier = Modifier.fillMaxWidth(), accent = Secondary) {
            Column(Modifier.padding(Spacing.xl)) {
                Text(t.theArgument,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                Spacer(Modifier.height(Spacing.sm))
                Text(uiState.promptArgument,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.56f)
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AssistChip(onClick = {}, label = { Text(String.format(t.timeSec, uiState.timeLimitSec)) },
                leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) })
            AssistChip(onClick = {}, label = {
                Text(when (uiState.difficulty) {
                    "easy" -> t.easy; "medium" -> t.medium; "hard" -> t.hard
                    else -> uiState.difficulty.replaceFirstChar { it.uppercase() }
                })
            },
                leadingIcon = { Icon(Icons.Filled.Speed, null, Modifier.size(16.dp)) })
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = { viewModel.startTimer() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = Radii.mediumShape,
            colors = glassButtonColors()
        ) {
            Icon(Icons.Filled.Timer, null)
            Spacer(Modifier.width(Spacing.sm))
            Text(t.startTimer)
        }
    }
}

// ============================================================
// RESPONDING — with circular timer progress
// ============================================================

@Composable
private fun RespondingPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val t = LocalTranslation.current
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
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accent = if (urgency) errorColor else primaryColor
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
            GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
                Column(Modifier.padding(Spacing.md)) {
                    Text(uiState.promptArgument,
                        style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    AiGeneratedDisclaimer(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Response input
            OutlinedTextField(
                value = uiState.userResponse,
                onValueChange = { viewModel.onResponseChanged(it) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text(t.writeRebuttal) },
                placeholder = { Text(t.rebuttalPlaceholder) },
                minLines = 5,
                shape = Radii.mediumShape,
                colors = glassTextFieldColors()
            )

            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = { viewModel.submitRebuttal() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = Radii.mediumShape,
                enabled = uiState.userResponse.isNotBlank(),
                colors = glassButtonColors()
            ) {
                Icon(Icons.Filled.Send, t.send)
                Spacer(Modifier.width(Spacing.sm))
                Text(t.submitRebuttal)
            }
        }
    }
}

// ============================================================
// SCORING
// ============================================================

@Composable
private fun ScoringPhase() {
    val t = LocalTranslation.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(Modifier.size(32.dp))
        Spacer(Modifier.height(Spacing.lg))
        Text(t.analyzingRebuttal, style = MaterialTheme.typography.bodyLarge)
    }
}

// ============================================================
// RESULT — with performance grade
// ============================================================

@Composable
private fun ResultPhase(uiState: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val t = LocalTranslation.current
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
    val displayGrade = when (grade) {
        "A" -> t.gradeA; "B" -> t.gradeB; "C" -> t.gradeC; else -> t.gradeD
    }
    val gradeColor = when (grade) {
        "A" -> SuccessGreen; "B" -> Tertiary; "C" -> WarningAmber
        else -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.lg).verticalScroll(rememberScrollState())
    ) {
        Text(t.scoreCard, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(Spacing.xl))

        GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(t.totalScore, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Spacer(Modifier.height(Spacing.sm))
                Text(String.format(t.scoreOutOf, displayScore),
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(Spacing.sm))
                Surface(shape = CircleShape, color = gradeColor.copy(alpha = 0.15f)) {
                    Text(displayGrade, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = gradeColor)
                }
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = 6.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.56f)
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AnimatedScoreChip(t.logic, attempt.logicScore, Modifier.weight(1f))
            AnimatedScoreChip(t.clarity, attempt.clarityScore, Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AnimatedScoreChip(t.persuasion, attempt.persuasionScore, Modifier.weight(1f))
            AnimatedScoreChip(t.evidenceLabel, attempt.evidenceScore, Modifier.weight(1f))
        }

        Spacer(Modifier.height(Spacing.xl))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            accent = Secondary
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                Text(t.feedback, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(Spacing.sm))
                Text(attempt.feedback, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.56f)
                )
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
                    Text(t.gettingBreakdown)
                } else {
                    Icon(Icons.Default.Info, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(t.explainScore)
                }
            }
        } else {
            // Per-category breakdown
            Spacer(Modifier.height(Spacing.lg))
            Text(t.detailedBreakdown, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        Text(t.overallAdvice, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(Modifier.height(Spacing.xs))
                        Text(uiState.explanation!!.overallAdvice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                        AiGeneratedDisclaimer(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.56f)
                        )
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
                        AiGeneratedDisclaimer(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.56f)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            // ———— CHAT SECTION ————
            Text(t.askAboutScore, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                    placeholder = { Text(t.askPlaceholder) },
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
                    Icon(Icons.Default.Send, t.send)
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
                    Text(t.timeTaken, style = MaterialTheme.typography.labelSmall)
                    Text(String.format(t.timeSec, attempt.timeTakenMs / 1000), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t.limit, style = MaterialTheme.typography.labelSmall)
                    Text(String.format(t.timeSec, attempt.timeLimitSec), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(onClick = { viewModel.newRound() }, modifier = Modifier.weight(1f)) {
                Text(t.newRound)
            }
            Button(onClick = { viewModel.startTimer() }, modifier = Modifier.weight(1f)) {
                Text(t.retrySame)
            }
        }
    }
}

// ============================================================
// COMPONENTS
// ============================================================

@Composable
private fun AnimatedScoreChip(label: String, score: Int, modifier: Modifier = Modifier) {
    val t = LocalTranslation.current
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
            Text(t.scoreDivider, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            AiGeneratedDisclaimer(
                modifier = Modifier.padding(top = 2.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.44f)
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    val t = LocalTranslation.current
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
            TextButton(onClick = onDismiss) { Text(t.dismissError) }
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
    val t = LocalTranslation.current
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
                Text("${breakdown.score}${t.scoreDivider}",
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
            AiGeneratedDisclaimer(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
            )
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
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(msg.content, style = MaterialTheme.typography.bodyMedium)
                if (!isUser) {
                    AiGeneratedDisclaimer(
                        modifier = Modifier.padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
                    )
                }
            }
        }
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins > 0) "$mins:${secs.toString().padStart(2, '0')}" else "${secs}s"
}
