package com.aidebate.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.theme.Spacing

@Composable
fun PhaseDivider(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .then(
                        Modifier.fillMaxWidth()
                    )
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(color = color.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f))
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = Spacing.md)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .then(
                        Modifier.fillMaxWidth()
                    )
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                    drawLine(color = color.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(size.width, 0f))
                }
            }
        }
    }
}
