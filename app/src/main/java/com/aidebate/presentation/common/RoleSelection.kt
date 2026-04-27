package com.aidebate.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.theme.*

@Composable
fun RoleSelectionCard(
    selected: Boolean,
    onClick: () -> Unit,
    role: DebateRole,
    label: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
) {
    val tokens = RoleTokenDefaults.forRole(role)
    val pressScale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1.0f,
        animationSpec = tween(200),
        label = "roleSelectionScale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .scale(pressScale),
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) tokens.color.container else tokens.color.container.copy(alpha = 0.3f),
        ),
        border = if (selected) BorderStroke(2.dp, tokens.color.primary) else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) tokens.depth.contentElevation.dp else 0.dp
        )
    ) {
        GlowWrapper(
            glowColor = tokens.color.glow,
            shape = Radii.mediumShape,
            isActive = selected,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) tokens.color.onContainer else tokens.color.onContainer.copy(alpha = 0.6f)
                )
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) tokens.color.onContainer.copy(alpha = 0.7f) else tokens.color.onContainer.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun RolePill(
    selected: Boolean,
    onClick: () -> Unit,
    role: DebateRole,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = RoleTokenDefaults.forRole(role)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) tokens.color.onContainer else tokens.color.onContainer.copy(alpha = 0.6f)
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = tokens.color.container.copy(alpha = 0.3f),
            selectedContainerColor = tokens.color.container,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = tokens.color.primary.copy(alpha = 0.3f),
            selectedBorderColor = tokens.color.primary,
            enabled = true,
            selected = selected,
        )
    )
}
