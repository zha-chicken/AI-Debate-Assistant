package com.aidebate.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.theme.MotionTween

@Composable
fun GlowWrapper(
    modifier: Modifier = Modifier,
    glowColor: Color,
    shape: Shape = RoundedCornerShape(16.dp),
    isActive: Boolean = false,
    content: @Composable () -> Unit,
) {
    val elevation by animateFloatAsState(
        targetValue = if (isActive) 8f else 0f,
        animationSpec = MotionTween.fadeIn,
        label = "glowElevation"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 0.35f else 0f,
        animationSpec = MotionTween.fadeIn,
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = glowColor.copy(alpha = alpha),
                spotColor = glowColor.copy(alpha = alpha),
            )
            .clip(shape)
    ) {
        content()
    }
}
