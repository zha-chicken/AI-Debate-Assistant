@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.constructive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.ConstructiveAnalysisIssue
import com.aidebate.presentation.common.AiGeneratedDisclaimer
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.AiBackdrop
import com.aidebate.presentation.theme.ErrorColor
import com.aidebate.presentation.theme.GlassCard
import com.aidebate.presentation.theme.GlassCardLevel
import com.aidebate.presentation.theme.Primary
import com.aidebate.presentation.theme.Radii
import com.aidebate.presentation.theme.Secondary
import com.aidebate.presentation.theme.Spacing
import com.aidebate.presentation.theme.SuccessGreen
import com.aidebate.presentation.theme.Tertiary
import com.aidebate.presentation.theme.WarningAmber
import com.aidebate.presentation.theme.glassButtonColors
import com.aidebate.presentation.theme.glassTextFieldColors
import com.aidebate.presentation.theme.glassTopAppBarColors

@Composable
fun ConstructiveAnalysisScreen(
    onBack: () -> Unit,
    viewModel: ConstructiveAnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.constructiveAnalysisTitle, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
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
                    t.constructiveAnalysisSub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    accent = Primary,
                    level = GlassCardLevel.PageGroup
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Campaign, contentDescription = null, tint = Primary)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                t.constructiveInputLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = viewModel::onTextChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 320.dp),
                            placeholder = { Text(t.constructivePlaceholder) },
                            minLines = 7,
                            shape = Radii.mediumShape,
                            colors = glassTextFieldColors()
                        )

                        Button(
                            onClick = viewModel::analyze,
                            enabled = uiState.inputText.isNotBlank() && !uiState.isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = Radii.mediumShape,
                            colors = glassButtonColors()
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(Spacing.sm))
                                Text(t.analyzing)
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(Modifier.width(Spacing.sm))
                                Text(t.analyzeConstructive)
                            }
                        }
                    }
                }

                uiState.errorMessage?.let { error ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = ErrorColor,
                        level = GlassCardLevel.Error
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(Spacing.lg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                AnimatedVisibility(
                    visible = uiState.hasAnalyzed && uiState.errorMessage == null,
                    enter = fadeIn() + expandVertically()
                ) {
                    ResultsSection(
                        issues = uiState.issues,
                        onClear = viewModel::clearResults
                    )
                }

                if (!uiState.hasAnalyzed && !uiState.isAnalyzing) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accent = Tertiary,
                        level = GlassCardLevel.PageGroup
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Psychology, contentDescription = null, tint = Tertiary)
                            Spacer(Modifier.width(Spacing.md))
                            Column {
                                Text(
                                    t.constructiveEmptyTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    t.constructiveEmptySubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun ResultsSection(
    issues: List<ConstructiveAnalysisIssue>,
    onClear: () -> Unit,
) {
    val t = LocalTranslation.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (issues.isEmpty()) t.noConstructiveIssues else String.format(t.constructiveClaimsFound, issues.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.material3.TextButton(onClick = onClear) {
                Text(t.clear)
            }
        }

        if (issues.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accent = SuccessGreen,
                level = GlassCardLevel.Result
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            t.noConstructiveIssues,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        AiGeneratedDisclaimer(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                        )
                    }
                }
            }
        } else {
            var expandedId by remember { mutableStateOf(issues.firstOrNull()?.id) }
            issues.forEach { issue ->
                ConstructiveIssueCard(
                    issue = issue,
                    expanded = expandedId == issue.id,
                    onClick = { expandedId = if (expandedId == issue.id) null else issue.id }
                )
            }
        }
    }
}

@Composable
private fun ConstructiveIssueCard(
    issue: ConstructiveAnalysisIssue,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val t = LocalTranslation.current
    val accent = when (issue.severity.lowercase()) {
        "high" -> ErrorColor
        "low" -> SuccessGreen
        else -> WarningAmber
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        level = if (expanded) GlassCardLevel.Focus else GlassCardLevel.Interactive,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(issue.issueType) }
                        )
                        Text(
                            issue.severity,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Text(
                        issue.claim,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        t.tapToViewRebuttalPoints,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (expanded) {
                if (issue.quote.isNotBlank()) {
                    AnalysisPanel(title = t.originalQuote, body = "\"${issue.quote}\"", accent = Secondary)
                }
                if (issue.explanation.isNotBlank()) {
                    AnalysisPanel(title = t.whyChallengeable, body = issue.explanation, accent = Primary)
                }
                if (issue.rebuttalPoints.isNotEmpty()) {
                    Text(
                        t.rebuttablePoints,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                    issue.rebuttalPoints.forEach { point ->
                        AnalysisPanel(title = null, body = point, accent = accent)
                    }
                }
                AiGeneratedDisclaimer(
                    modifier = Modifier.padding(top = Spacing.xs),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f)
                )
            }
        }
    }
}

@Composable
private fun AnalysisPanel(
    title: String?,
    body: String,
    accent: Color,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accent = accent,
        level = GlassCardLevel.PageGroup
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                fontStyle = if (title == null) FontStyle.Normal else FontStyle.Italic
            )
        }
    }
}
