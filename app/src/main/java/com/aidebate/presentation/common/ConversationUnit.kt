package com.aidebate.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.domain.model.DebateTurn
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

            // Provider attribution
            if (turn.providerUsed != null) {
                Text(
                    "${turn.providerUsed.displayName}${turn.modelUsed?.let { " / $it" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.color.onContainer.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp)
                )
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
