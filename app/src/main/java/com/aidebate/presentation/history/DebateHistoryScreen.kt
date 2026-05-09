@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.HistoryItem
import com.aidebate.domain.model.SessionStatus
import com.aidebate.presentation.common.TopLevelBottomBar
import com.aidebate.presentation.common.TopLevelDestination
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebateHistoryScreen(
    onDebateSelected: (HistoryItem.Debate) -> Unit,
    onRebuttalSelected: (String) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onTools: () -> Unit = {},
    onSettings: () -> Unit = {},
    onStartDebate: () -> Unit = onHome,
    viewModel: DebateHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filteredItems = remember(uiState.items, filter) {
        when (filter) {
            HistoryFilter.ALL -> uiState.items
            HistoryFilter.DEBATES -> uiState.items.filterIsInstance<HistoryItem.Debate>()
            HistoryFilter.TRAINING -> uiState.items.filterIsInstance<HistoryItem.Rebuttal>()
        }
    }

    LaunchedEffect(Unit) { viewModel.loadSessions() }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.history, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Default.Tune, t.settings)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            },
            bottomBar = {
                HistoryBottomBar(
                    onHome = onHome,
                    onHistory = {},
                    onTools = onTools,
                    onSettings = onSettings,
                )
            }
        ) { padding ->
            if (uiState.isEmpty) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            t.noActivityYet,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            t.noActivitySubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Button(onClick = onStartDebate, colors = glassButtonColors()) {
                                Text(t.startDebate)
                            }
                            OutlinedButton(onClick = onTools) {
                                Text(t.toolsTitle)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    item(key = "filters") {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            HistoryFilterChip("All", filter == HistoryFilter.ALL) { filter = HistoryFilter.ALL }
                            HistoryFilterChip("Debates", filter == HistoryFilter.DEBATES) { filter = HistoryFilter.DEBATES }
                            HistoryFilterChip("Training", filter == HistoryFilter.TRAINING) { filter = HistoryFilter.TRAINING }
                        }
                    }
                    items(filteredItems, key = { it.id }) { item ->
                        when (item) {
                            is HistoryItem.Debate -> DebateHistoryCard(
                                item = item,
                                onClick = { onDebateSelected(item) },
                                onDelete = { viewModel.deleteSession(item.id) }
                            )
                            is HistoryItem.Rebuttal -> RebuttalHistoryCard(
                                item = item,
                                onClick = { onRebuttalSelected(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class HistoryFilter { ALL, DEBATES, TRAINING }

@Composable
private fun HistoryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = GlassSurface,
            selectedContainerColor = GlassSurfaceStrong,
            labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = GlassStroke,
            selectedBorderColor = Primary.copy(alpha = 0.8f)
        )
    )
}

@Composable
private fun DebateHistoryCard(
    item: HistoryItem.Debate,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalTranslation.current
    var showDelete by remember { mutableStateOf(false) }
    val summary = item.summary

    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accent = Primary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = Radii.smallShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        t.debateBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    summary.topicTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Surface(
                        shape = Radii.smallShape,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            if (summary.mode == DebateMode.USER_VS_AI) t.userVsAi else t.aiVsAi,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = Radii.smallShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            String.format(t.turns, summary.turnCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (summary.status != SessionStatus.ACTIVE) {
                        Surface(
                            shape = Radii.smallShape,
                            color = when (summary.status) {
                                SessionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                summary.status.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (summary.status) {
                                    SessionStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    formatDate(summary.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            IconButton(onClick = { showDelete = true }) {
                Icon(
                    Icons.Default.Delete, t.delete,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(t.deleteConfirmTitle) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text(t.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun RebuttalHistoryCard(
    item: HistoryItem.Rebuttal,
    onClick: () -> Unit
) {
    val t = LocalTranslation.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accent = Tertiary
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = Radii.smallShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        t.rebuttalPractice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Surface(
                        shape = Radii.smallShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            item.session.difficulty.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.bestScore != null) {
                        val scoreColor = when {
                            item.bestScore >= 75 -> SuccessGreen
                            item.bestScore >= 50 -> WarningAmber
                            else -> MaterialTheme.colorScheme.error
                        }
                        Surface(
                            shape = Radii.smallShape,
                            color = scoreColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                String.format(t.bestScore, item.bestScore),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.attemptCount > 0) {
                        Text(
                            String.format(t.attemptCount, item.attemptCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    formatDate(item.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun HistoryBottomBar(
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit,
) {
    TopLevelBottomBar(
        selected = TopLevelDestination.History,
        onHome = onHome,
        onHistory = onHistory,
        onTools = onTools,
        onSettings = onSettings,
    )
}

private fun formatDate(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) { "" }
}
