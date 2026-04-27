@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNewDebate: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onArgumentMap: () -> Unit = {},
    onRebuttalTrainer: () -> Unit = {},
    onFallacyDetector: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("AI Debate", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.md))

            // Hero section
            HeroSection()
            Spacer(Modifier.height(Spacing.xxl))

            // Debate Actions
            SectionHeader("Debate Actions")
            StaggeredCard(
                index = 0,
                title = "New Debate",
                subtitle = "Start a new debate session with AI",
                icon = Icons.Filled.PlayArrow,
                onClick = onNewDebate,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                accentColor = MaterialTheme.colorScheme.primary
            )
            StaggeredCard(
                index = 1,
                title = "Debate History",
                subtitle = "Review past debates and transcripts",
                icon = Icons.Filled.History,
                onClick = onHistory,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                accentColor = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(Spacing.xl))

            // Preparation Tools
            SectionHeader("Preparation Tools")
            StaggeredCard(
                index = 2,
                title = "Argument Map",
                subtitle = "Visual mindmap of pro & con arguments",
                icon = Icons.Filled.AccountTree,
                onClick = onArgumentMap,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                accentColor = MaterialTheme.colorScheme.tertiary
            )
            StaggeredCard(
                index = 3,
                title = "Rebuttal Trainer",
                subtitle = "Practice rebuttals under timed pressure",
                icon = Icons.Filled.Timer,
                onClick = onRebuttalTrainer,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                accentColor = MaterialTheme.colorScheme.primary
            )
            StaggeredCard(
                index = 4,
                title = "Fallacy Detector",
                subtitle = "Identify logical fallacies in arguments",
                icon = Icons.Filled.Search,
                onClick = onFallacyDetector,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                accentColor = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(Spacing.lg))

            // Settings
            StaggeredCard(
                index = 5,
                title = "Settings",
                subtitle = "Configure AI providers and models",
                icon = Icons.Filled.Settings,
                onClick = onSettings,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                accentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
    )
}

@Composable
private fun HeroSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")

    // Pulse animation
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Slow rotation for gradient ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(contentAlignment = Alignment.Center) {
        // Gradient ring (behind icon)
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(pulse * 1.05f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                primary,
                                tertiary,
                                secondary,
                                primary
                            ),
                        ),
                        radius = size.minDimension / 2f,
                        alpha = 0.6f,
                    )
                }
        )

        // Main icon circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Forum, null,
                Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    Text(
        "Welcome to AI Debate",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Text(
        "Challenge AI or watch two AIs debate each other",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = Spacing.xs)
    )
}

@Composable
private fun StaggeredCard(
    index: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, delayMillis = index * 50)) +
            slideInVertically(tween(500, easing = EaseOutCubic)) { it / 3 }
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            shape = Radii.mediumShape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = accentColor
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.65f)
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight, null,
                    Modifier.size(20.dp),
                    tint = contentColor.copy(alpha = 0.3f)
                )
            }
        }
    }
}
