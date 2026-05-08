@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.home

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.AiBackdrop
import com.aidebate.presentation.theme.GlassCard
import com.aidebate.presentation.theme.GlassSurfaceStrong
import com.aidebate.presentation.theme.Primary
import com.aidebate.presentation.theme.Radii
import com.aidebate.presentation.theme.Secondary
import com.aidebate.presentation.theme.Spacing
import com.aidebate.presentation.theme.Tertiary
import com.aidebate.presentation.theme.WarmGlow
import com.aidebate.presentation.theme.glassTopAppBarColors
import com.aidebate.presentation.theme.softCircle

@Composable
fun HomeScreen(
    onNewDebate: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onDonation: () -> Unit = {},
    onArgumentMap: () -> Unit = {},
    onRebuttalTrainer: () -> Unit = {},
    onFallacyDetector: () -> Unit = {},
    onFaceToFace: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val t = LocalTranslation.current
    val stats by viewModel.stats.collectAsState()

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            t.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Menu, contentDescription = t.settings)
                        }
                    },
                    actions = {
                        IconButton(onClick = onDonation) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = t.supportDevelopment, tint = WarmGlow)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            },
            bottomBar = {
                HomeBottomBar(
                    onHome = {},
                    onHistory = onHistory,
                    onTools = onArgumentMap,
                    onProfile = onSettings,
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(Spacing.md))
                HeroSection()
                Spacer(Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatCard(stats.debateCount.toString(), t.debateCount, Icons.Filled.EmojiEvents, Primary, Modifier.weight(1f))
                    StatCard("${stats.winRatePercent}%", t.winRate, Icons.Filled.Settings, Tertiary, Modifier.weight(1f))
                    StatCard(stats.winStreak.toString(), t.winStreak, Icons.Filled.LocalFireDepartment, WarmGlow, Modifier.weight(1f))
                }

                Spacer(Modifier.height(Spacing.md))
                SupportStrip(onDonation)
                Spacer(Modifier.height(Spacing.lg))

                SectionHeader(t.sectionDebateActions)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    ActionTile(
                        title = t.newDebate,
                        subtitle = t.newDebateSubtitle,
                        icon = Icons.Filled.Forum,
                        accent = Primary,
                        onClick = onNewDebate,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = t.faceToFace,
                        subtitle = t.faceToFaceSubtitle,
                        icon = Icons.Filled.Psychology,
                        accent = Secondary,
                        onClick = onFaceToFace,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                WideAction(
                    title = t.debateHistory,
                    subtitle = t.debateHistorySubtitle,
                    icon = Icons.Filled.History,
                    accent = Tertiary,
                    onClick = onHistory,
                )

                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(t.sectionPrepTools)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    ToolTile(t.argumentMap, Icons.Filled.AccountTree, Secondary, onArgumentMap, Modifier.weight(1f))
                    ToolTile(t.rebuttalTrainer, Icons.Filled.Timer, WarmGlow, onRebuttalTrainer, Modifier.weight(1f))
                    ToolTile(t.fallacyDetector, Icons.Filled.Search, Tertiary, onFallacyDetector, Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun HeroSection() {
    val t = LocalTranslation.current
    val transition = rememberInfiniteTransition(label = "homeHero")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(164.dp)
            .scale(pulse),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            listOf(0.45f, 0.62f, 0.79f).forEachIndexed { index, factor ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f - index * 0.015f),
                    radius = size.minDimension * factor / 2f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            drawArc(
                color = WarmGlow.copy(alpha = 0.55f),
                startAngle = rotation,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, size.height * 0.64f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .softCircle(GlassSurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(38.dp))
        }
    }

    Text(
        t.welcomeTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
    Text(
        t.welcomeSubtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = Spacing.xs)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun StatCard(value: String, label: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.height(72.dp), accent = accent) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = accent)
                Spacer(Modifier.width(Spacing.xs))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}

@Composable
private fun SupportStrip(onDonation: () -> Unit) {
    val t = LocalTranslation.current
    GlassCard(onClick = onDonation, accent = Tertiary, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).softCircle(Tertiary.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, null, tint = Tertiary)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(t.supportDevelopment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(t.supportSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
            }
            Text(t.donate, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
            Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f))
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(onClick = onClick, accent = accent, modifier = modifier.aspectRatio(1.08f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.md),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(Modifier.size(42.dp).softCircle(accent.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), maxLines = 2)
            }
        }
    }
}

@Composable
private fun WideAction(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    GlassCard(onClick = onClick, accent = accent, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).softCircle(accent.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
    }
}

@Composable
private fun ToolTile(title: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(onClick = onClick, accent = accent, modifier = modifier.height(96.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accent)
            Spacer(Modifier.height(Spacing.sm))
            Text(title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
private fun HomeBottomBar(
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onTools: () -> Unit,
    onProfile: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            .fillMaxWidth()
            .height(58.dp)
            .background(GlassSurfaceStrong, Radii.largeShape)
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val t = LocalTranslation.current
        BottomItem(Icons.Filled.Forum, t.homeTab, true, onHome)
        BottomItem(Icons.Filled.History, t.debatesTab, false, onHistory)
        BottomItem(Icons.Filled.AccountTree, t.toolsTab, false, onTools)
        BottomItem(Icons.Filled.Shield, t.profileTab, false, onProfile)
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = if (selected) WarmGlow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) WarmGlow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
    }
}
