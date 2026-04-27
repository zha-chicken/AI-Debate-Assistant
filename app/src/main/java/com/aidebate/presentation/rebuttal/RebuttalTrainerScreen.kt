@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.rebuttal

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.theme.SuccessGreen
import com.aidebate.presentation.theme.WarningAmber

@Composable
fun RebuttalTrainerScreen(
    onBack: () -> Unit,
    viewModel: RebuttalTrainerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            AnimatedContent(
                targetState = uiState.phase,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(400)) { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { -it / 4 })
                },
                label = "phase"
            ) { phase ->
                when (phase) {
                    TrainerPhase.TOPIC_SELECT -> TopicSelectPhase(uiState, viewModel)
                    TrainerPhase.SETUP -> SetupPhase(uiState, viewModel)
                    TrainerPhase.READY -> ReadyPhase(uiState, viewModel)
                    TrainerPhase.RESPONDING -> RespondingPhase(uiState, viewModel)
                    TrainerPhase.SCORING -> ScoringPhase()
                    TrainerPhase.RESULT -> ResultPhase(uiState, viewModel)
                }
            }

            // Loading overlay (only for argument generation, scoring uses its own phase)
            AnimatedVisibility(
                visible = uiState.isGenerating,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Generating argument...",
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
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
private fun TopicSelectPhase(state: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Select a Topic", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Choose a debate topic to practice rebuttals",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(20.dp))

        state.topics.forEach { topic ->
            Card(
                onClick = { viewModel.selectTopic(topic.id, topic.title) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Topic, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(topic.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (topic.category.isNotBlank())
                            Text(topic.category, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Icon(Icons.Filled.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        }

        if (state.topics.isEmpty()) {
            Text("No topics available. Create a topic first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 40.dp).fillMaxWidth(),
                textAlign = TextAlign.Center)
        }

        if (state.sessions.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Past Sessions", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            state.sessions.take(5).forEach { session ->
                Card(
                    onClick = { viewModel.selectSession(session.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.width(8.dp))
                        Text(session.topicTitle, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f))
                        Text(session.userSide, style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (session.userSide == "FOR") SuccessGreen else MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupPhase(state: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Session Setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Topic: ${state.selectedTopicTitle}",
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(24.dp))

        // Side
        SectionHeader("Your Side")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SideChip("FOR", state.userSide == "FOR") { viewModel.setSide("FOR") }
            SideChip("AGAINST", state.userSide == "AGAINST") { viewModel.setSide("AGAINST") }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Difficulty")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("easy" to "Easy", "medium" to "Medium", "hard" to "Hard").forEach { (v, l) ->
                FilterChip(selected = state.difficulty == v, onClick = { viewModel.setDifficulty(v) },
                    label = { Text(l) })
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("Time Limit")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(30 to "30s", 60 to "60s", 90 to "90s").forEach { (s, l) ->
                FilterChip(
                    selected = state.timeLimitSec == s,
                    onClick = { viewModel.setTimeLimit(s) },
                    label = { Text(l) },
                    leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.startSession() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text("Start Training")
        }
    }
}

@Composable
private fun ReadyPhase(state: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Your Argument", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Read and prepare. Start the timer when ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("THE ARGUMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Text(state.promptArgument,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("${state.timeLimitSec}s") },
                leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(16.dp)) })
            AssistChip(onClick = {}, label = { Text(state.difficulty.replaceFirstChar { it.uppercase() }) },
                leadingIcon = { Icon(Icons.Filled.Speed, null, Modifier.size(16.dp)) })
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.startTimer() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Timer, null)
            Spacer(Modifier.width(8.dp))
            Text("Start Timer & Write Rebuttal")
        }
    }
}

@Composable
private fun RespondingPhase(state: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val urgency = state.timeRemainingSec <= 10
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Live timer
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (urgency) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, null,
                    tint = if (urgency) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTimer(state.timeRemainingSec),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (urgency) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(state.promptArgument, Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.userResponse,
            onValueChange = { viewModel.onResponseChanged(it) },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Write your rebuttal...") },
            placeholder = { Text("Type your counter-argument here...") },
            minLines = 5,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.submitRebuttal() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = state.userResponse.isNotBlank()
        ) {
            Icon(Icons.Filled.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("Submit Rebuttal")
        }
    }
}

@Composable
private fun ScoringPhase() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text("Analyzing your rebuttal...", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ResultPhase(state: RebuttalTrainerUiState, viewModel: RebuttalTrainerViewModel) {
    val attempt = state.currentAttempt ?: return

    // Animated score counter
    val displayScore by animateIntAsState(
        targetValue = attempt.totalScore,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "totalScore"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())
    ) {
        Text("Score Card", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TOTAL SCORE", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                Text("$displayScore / 100",
                    style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sub-scores
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedScoreChip("Logic", attempt.logicScore, Modifier.weight(1f))
            AnimatedScoreChip("Clarity", attempt.clarityScore, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnimatedScoreChip("Persuasion", attempt.persuasionScore, Modifier.weight(1f))
            AnimatedScoreChip("Evidence", attempt.evidenceScore, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // Feedback
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Feedback", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(8.dp))
                Text(attempt.feedback, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
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

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.newRound() }, modifier = Modifier.weight(1f)) {
                Text("New Round")
            }
            Button(onClick = { viewModel.startTimer() }, modifier = Modifier.weight(1f)) {
                Text("Retry Same")
            }
        }
    }
}

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
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$displayScore", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = when {
                    score >= 20 -> SuccessGreen
                    score >= 15 -> WarningAmber
                    else -> MaterialTheme.colorScheme.error
                })
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("/25", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SideChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = when (label) {
        "FOR" -> SuccessGreen
        else -> MaterialTheme.colorScheme.error
    }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        },
        leadingIcon = {
            if (selected) Icon(
                if (label == "FOR") Icons.Filled.ThumbUp else Icons.Filled.ThumbDown,
                null, Modifier.size(16.dp), tint = color
            )
        }
    )
}

private fun formatTimer(totalSeconds: Int): String {
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    return if (mins > 0) "$mins:${secs.toString().padStart(2, '0')}" else "${secs}s"
}
