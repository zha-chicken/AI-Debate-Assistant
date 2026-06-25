@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aidebate.presentation.common.TopLevelBottomBar
import com.aidebate.presentation.common.TopLevelDestination
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.AiBackdrop
import com.aidebate.presentation.theme.GlassCard
import com.aidebate.presentation.theme.GlassCardLevel
import com.aidebate.presentation.theme.Primary
import com.aidebate.presentation.theme.RhetorixAccents
import com.aidebate.presentation.theme.Spacing
import com.aidebate.presentation.theme.Tertiary
import com.aidebate.presentation.theme.WarmGlow
import com.aidebate.presentation.theme.glassTopAppBarColors
import com.aidebate.presentation.theme.softCircle

private const val HALLUCINATION_DETECTOR_URL = "https://gptzero.me/hallucination-detector"

private sealed interface ToolDestination {
    data object Internal : ToolDestination
    data class External(val url: String) : ToolDestination
}

private data class ToolCardUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val requiresAiProvider: Boolean,
    val isExternalLink: Boolean,
    val destination: ToolDestination,
)

@Composable
fun ToolsScreen(
    onArgumentGraph: () -> Unit,
    onRebuttalTrainer: () -> Unit,
    onFallacyDetector: () -> Unit,
    onHome: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val t = LocalTranslation.current
    val uriHandler = LocalUriHandler.current
    val tools = listOf(
        ToolCardUi(
            id = "constructive_analysis",
            title = t.argumentMap,
            subtitle = t.argumentMapSubtitle,
            icon = Icons.Filled.Psychology,
            accent = Primary,
            requiresAiProvider = true,
            isExternalLink = false,
            destination = ToolDestination.Internal,
        ),
        ToolCardUi(
            id = "rebuttal_trainer",
            title = t.rebuttalTrainer,
            subtitle = t.rebuttalTrainerSubtitle,
            icon = Icons.Filled.Timer,
            accent = WarmGlow,
            requiresAiProvider = true,
            isExternalLink = false,
            destination = ToolDestination.Internal,
        ),
        ToolCardUi(
            id = "fallacy_detector",
            title = t.fallacyDetector,
            subtitle = t.fallacyDetectorSubtitle,
            icon = Icons.Filled.Search,
            accent = Tertiary,
            requiresAiProvider = true,
            isExternalLink = false,
            destination = ToolDestination.Internal,
        ),
        ToolCardUi(
            id = "hallucination_detector",
            title = t.hallucinationDetector,
            subtitle = t.hallucinationDetectorSubtitle,
            icon = Icons.Filled.Psychology,
            accent = RhetorixAccents.Lavender,
            requiresAiProvider = false,
            isExternalLink = true,
            destination = ToolDestination.External(HALLUCINATION_DETECTOR_URL),
        ),
    )

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.toolsTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            },
            bottomBar = {
                TopLevelBottomBar(
                    selected = TopLevelDestination.Tools,
                    onHome = onHome,
                    onHistory = onHistory,
                    onTools = {},
                    onSettings = onSettings,
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    t.toolsSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )

                tools.forEach { tool ->
                    ToolCard(
                        tool = tool,
                        onClick = {
                            when (tool.id) {
                                "constructive_analysis" -> onArgumentGraph()
                                "rebuttal_trainer" -> onRebuttalTrainer()
                                "fallacy_detector" -> onFallacyDetector()
                                "hallucination_detector" -> uriHandler.openUri(HALLUCINATION_DETECTOR_URL)
                            }
                        },
                        trailingText = when {
                            tool.isExternalLink -> t.openExternalTool
                            tool.requiresAiProvider -> "Requires AI"
                            else -> "AI optional"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    tool: ToolCardUi,
    onClick: () -> Unit,
    trailingText: String? = null,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = tool.accent,
        level = GlassCardLevel.Interactive,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .softCircle(tool.accent.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    tool.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
            if (trailingText != null) {
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                if (tool.isExternalLink) Icons.Filled.OpenInNew else Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = tool.accent.copy(alpha = 0.82f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
