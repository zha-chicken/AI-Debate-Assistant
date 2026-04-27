package com.aidebate.presentation.common

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.theme.*

@Composable
fun SmartTypingIndicator(
    roleName: String = "AI",
    roleTokens: RoleTokens = RoleTokenDefaults.Pro,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "smartTyping")

    // Dots with horizontal offset animation
    val dotOffsets = listOf(
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                tween(600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot1"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                tween(600, 150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot2"
        ),
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                tween(600, 300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot3"
        ),
    )

    val dotAlphas = listOf(
        infiniteTransition.animateFloat(
            0.4f, 1f,
            infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
            label = "da1"
        ),
        infiniteTransition.animateFloat(
            0.4f, 1f,
            infiniteRepeatable(tween(500, 150), repeatMode = RepeatMode.Reverse),
            label = "da2"
        ),
        infiniteTransition.animateFloat(
            0.4f, 1f,
            infiniteRepeatable(tween(500, 300), repeatMode = RepeatMode.Reverse),
            label = "da3"
        ),
    )

    Column(modifier = modifier) {
        // Status text
        Text(
            text = "$roleName is constructing argument…",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = roleTokens.color.primary.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Animated dots
        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = roleTokens.color.container.copy(alpha = 0.5f),
            modifier = Modifier.widthIn(max = 80.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .scale(dotAlphas[i].value)
                            .alpha(dotAlphas[i].value)
                            .offset(x = dotOffsets[i].value.dp)
                            .clip(CircleShape)
                            .background(roleTokens.color.primary.copy(alpha = 0.6f))
                    )
                    if (i < 2) Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}
