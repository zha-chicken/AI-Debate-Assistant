@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AiBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(WarmGlow.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.74f, size.height * 0.12f),
                    radius = size.minDimension * 0.72f,
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.74f, size.height * 0.12f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Tertiary.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.92f),
                    radius = size.minDimension * 0.75f,
                ),
                radius = size.minDimension * 0.75f,
                center = Offset(size.width * 0.12f, size.height * 0.92f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.035f),
                radius = size.minDimension * 0.35f,
                center = Offset(size.width * 0.52f, size.height * 0.18f),
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.025f),
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.52f, size.height * 0.18f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = Primary,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = Radii.mediumShape
    val colors = CardDefaults.cardColors(
        containerColor = if (selected) GlassSurfaceStrong else GlassSurface
    )
    val border = BorderStroke(
        width = 1.dp,
        color = if (selected) accent.copy(alpha = 0.85f) else GlassStroke
    )
    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = { content() }
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = { content() }
        )
    }
}

fun Modifier.glassPanel(
    radius: androidx.compose.ui.unit.Dp = Radii.medium,
    strokeColor: Color = GlassStroke,
): Modifier = clip(RoundedCornerShape(radius))
    .background(GlassSurface)
    .border(1.dp, strokeColor, RoundedCornerShape(radius))

fun Modifier.softCircle(
    color: Color = GlassSurfaceStrong,
): Modifier = clip(CircleShape).background(color)

@Composable
fun glassTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = SurfaceDark.copy(alpha = 0.92f),
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
fun glassTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = GlassSurface,
    unfocusedContainerColor = GlassSurface,
    disabledContainerColor = GlassSurface.copy(alpha = 0.45f),
    focusedBorderColor = Primary.copy(alpha = 0.8f),
    unfocusedBorderColor = GlassStroke,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    focusedLabelColor = PrimaryLight,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
)

@Composable
fun glassButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Primary.copy(alpha = 0.72f),
    contentColor = contentColorFor(Primary),
    disabledContainerColor = GlassSurface,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f),
)
