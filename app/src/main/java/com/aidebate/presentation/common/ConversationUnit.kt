package com.aidebate.presentation.common

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aidebate.domain.model.ArgumentHighlight
import com.aidebate.domain.model.DebateTurn
import com.aidebate.domain.model.HighlightType
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.presentation.theme.*

@Composable
fun ConversationUnit(
    turn: DebateTurn,
    importance: TurnImportance = TurnImportance.NORMAL,
    modifier: Modifier = Modifier,
) {
    val isUser = turn.speakerRole == SpeakerRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val role = turn.speakerRole.toDebateRole()
    val tokens = RoleTokenDefaults.forRole(role)
    val label = tokens.label

    val importanceScale = when (importance) {
        TurnImportance.STRONG -> 1.0f
        TurnImportance.NORMAL -> 0.98f
        TurnImportance.WEAK -> 0.96f
    }
    val importanceAlpha = when (importance) {
        TurnImportance.STRONG -> 1.0f
        TurnImportance.NORMAL -> 0.9f
        TurnImportance.WEAK -> 0.7f
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(turn.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(tokens.motion.entryDurationMs)) +
                slideInVertically(
                    animationSpec = tween(tokens.motion.entryDurationMs, easing = tokens.motion.entryEasing),
                    initialOffsetY = { it / 2 }
                )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .scale(importanceScale)
                .then(if (importanceAlpha < 1f) Modifier else Modifier),
            horizontalAlignment = alignment
        ) {
            // Role label row
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isUser) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(tokens.color.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.color.onContainer
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.color.onContainer
                    )
                } else {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tokens.color.onContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(tokens.color.container),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label.take(1),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.color.onContainer
                        )
                    }
                }
            }

            // Gradient-filled bubble
            GlowWrapper(
                glowColor = tokens.color.glow,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                isActive = importance == TurnImportance.STRONG
            ) {
                Surface(
                    modifier = Modifier.widthIn(max = 320.dp),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = tokens.color.container.copy(alpha = importanceAlpha)
                ) {
                    Text(
                        text = turn.content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.color.onContainer
                    )
                }
            }

            // AI-generated content disclaimer
            if (!isUser) {
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = 4.dp),
                    color = tokens.color.onContainer.copy(alpha = 0.52f)
                )
            }

            // Provider attribution
            if (turn.providerUsed != null) {
                Text(
                    "${turn.providerUsed.displayName}${turn.modelUsed?.let { " / $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.color.onContainer.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Score badge (appears asynchronously after AI scores the turn)
            AnimatedVisibility(
                visible = turn.score != null,
                enter = fadeIn(tween(400)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                turn.score?.let { score ->
                    ScoreBadge(overall = score.overall, rationale = score.rationale)
                }
            }

            // Highlight quotes
            if (!turn.highlights.isNullOrEmpty()) {
                turn.highlights.forEach { highlight ->
                    HighlightQuote(highlight = highlight)
                }
            }
        }
    }
}

enum class TurnImportance { STRONG, NORMAL, WEAK }

fun SpeakerRole.toDebateRole(): DebateRole = when (this) {
    SpeakerRole.AI_PROPOSITION -> DebateRole.PRO
    SpeakerRole.AI_OPPOSITION -> DebateRole.CON
    SpeakerRole.USER -> DebateRole.USER
    SpeakerRole.MODERATOR -> DebateRole.MODERATOR
}

// ============================================================
// SCORE BADGE — shown below scored turns
// ============================================================

private val ScoreGreen = Color(0xFF2E7D32)
private val ScoreAmber = Color(0xFFF57F17)
private val ScoreRed = Color(0xFFC62828)

@Composable
private fun ScoreBadge(overall: Int, rationale: String) {
    val scoreColor = when {
        overall >= 70 -> ScoreGreen
        overall >= 40 -> ScoreAmber
        else -> ScoreRed
    }

    var expanded by remember { mutableStateOf(false) }

    Spacer(Modifier.height(4.dp))
    Surface(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        color = scoreColor.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = scoreColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$overall/100",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
                if (rationale.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreColor.copy(alpha = 0.5f)
                    )
                }
            }
            AnimatedVisibility(visible = expanded && rationale.isNotBlank()) {
                Text(
                    rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            AiGeneratedDisclaimer(
                modifier = Modifier.padding(top = 2.dp),
                color = scoreColor.copy(alpha = 0.62f)
            )
        }
    }
}

// ============================================================
// HIGHLIGHT QUOTE — shows AI-identified argument highlights
// ============================================================

@Composable
private fun HighlightQuote(highlight: ArgumentHighlight) {
    val highlightColor = when (highlight.type) {
        HighlightType.STRONG_ARGUMENT -> Color(0xFF2E7D32)
        HighlightType.WEAK_EVIDENCE -> Color(0xFFC62828)
        HighlightType.LOGICAL_FALLACY -> Color(0xFFE65100)
        HighlightType.CRITICAL_FLAW -> Color(0xFFB71C1C)
        HighlightType.NOTABLE_INSIGHT -> Color(0xFF1565C0)
    }
    val bgColor = when (highlight.type) {
        HighlightType.STRONG_ARGUMENT -> Color(0xFFE8F5E9)
        HighlightType.WEAK_EVIDENCE -> Color(0xFFFFEBEE)
        HighlightType.LOGICAL_FALLACY -> Color(0xFFFBE9E7)
        HighlightType.CRITICAL_FLAW -> Color(0xFFFCE4EC)
        HighlightType.NOTABLE_INSIGHT -> Color(0xFFE3F2FD)
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        highlight.type.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = highlightColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "· ${highlight.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = highlightColor.copy(alpha = 0.7f)
                    )
                }
                if (highlight.quotedText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "\"${highlight.quotedText}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = 4.dp),
                    color = highlightColor.copy(alpha = 0.62f)
                )
            }
        }
    }
}
