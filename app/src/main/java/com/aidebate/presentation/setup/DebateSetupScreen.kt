@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.setup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.DebateDifficulty
import com.aidebate.domain.model.DebateFormat
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.presentation.common.RolePill
import com.aidebate.presentation.common.RoleSelectionCard
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*

@Composable
fun DebateSetupScreen(
    topicId: String,
    onStartDebate: (String) -> Unit,
    onStartFaceToFace: (String) -> Unit = {},
    onBack: () -> Unit,
    viewModel: DebateSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    LaunchedEffect(topicId) { viewModel.initialize(topicId) }

    val sessionId = viewModel.sessionId.collectAsState()
    val f2fSessionId = viewModel.f2fSessionId.collectAsState()

    LaunchedEffect(sessionId.value) {
        sessionId.value?.let { onStartDebate(it) }
    }

    LaunchedEffect(f2fSessionId.value) {
        f2fSessionId.value?.let { onStartFaceToFace(it) }
    }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.debateSetup, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    colors = glassTopAppBarColors()
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    // Topic info
                    TopicCard(uiState.topicTitle)

                    // Debate Mode
                    SectionLabel(t.debateMode)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        ModeCard(
                            title = t.userVsAi,
                            icon = Icons.Default.Person,
                            selected = uiState.selectedMode == DebateMode.USER_VS_AI,
                            onClick = { viewModel.onModeSelected(DebateMode.USER_VS_AI) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeCard(
                            title = t.aiVsAi,
                            icon = Icons.Default.Adb,
                            selected = uiState.selectedMode == DebateMode.AI_VS_AI,
                            onClick = { viewModel.onModeSelected(DebateMode.AI_VS_AI) },
                            modifier = Modifier.weight(1f)
                        )
                        ModeCard(
                            title = t.faceToFaceLabel,
                            icon = Icons.Default.Forum,
                            selected = uiState.selectedMode == DebateMode.USER_VS_USER,
                            onClick = { viewModel.onModeSelected(DebateMode.USER_VS_USER) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                // Face-to-Face description (only for F2F)
                AnimatedVisibility(visible = uiState.selectedMode == DebateMode.USER_VS_USER) {
                    GlassCard(
                        accent = Primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.People, null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                t.f2fSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Debate Format (hidden for F2F — always structured)
                AnimatedVisibility(visible = uiState.selectedMode != DebateMode.USER_VS_USER) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        SectionLabel(t.format)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            ModeCard(
                                title = t.structured,
                                subtitle = t.structuredSubtitle,
                                icon = Icons.Default.Segment,
                                selected = uiState.selectedFormat == DebateFormat.STRUCTURED,
                                onClick = { viewModel.onFormatSelected(DebateFormat.STRUCTURED) },
                                modifier = Modifier.weight(1f)
                            )
                            ModeCard(
                                title = t.freeFlow,
                                subtitle = t.freeFlowSubtitle,
                                icon = Icons.Default.Chat,
                                selected = uiState.selectedFormat == DebateFormat.FREE_FLOW,
                                onClick = { viewModel.onFormatSelected(DebateFormat.FREE_FLOW) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // AI Difficulty (hidden for F2F)
                AnimatedVisibility(visible = uiState.selectedMode != DebateMode.USER_VS_USER) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        SectionLabel(t.aiDifficulty)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            DebateDifficulty.entries.forEach { difficulty ->
                                val label = when (difficulty) {
                                    DebateDifficulty.EASY -> t.easy
                                    DebateDifficulty.MEDIUM -> t.medium
                                    DebateDifficulty.HARD -> t.hard
                                }
                                ModeCard(
                                    title = label,
                                    selected = uiState.selectedDifficulty == difficulty,
                                    onClick = { viewModel.onDifficultySelected(difficulty) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // User position (only for User vs AI)
                AnimatedVisibility(visible = uiState.selectedMode == DebateMode.USER_VS_AI) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        SectionLabel(t.yourPosition)
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            RoleSelectionCard(
                                selected = uiState.userSide == SpeakerRole.AI_PROPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_PROPOSITION) },
                                role = com.aidebate.presentation.theme.DebateRole.PRO,
                                label = t.argueFor,
                                subtitle = t.argueFor,
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectionCard(
                                selected = uiState.userSide == SpeakerRole.AI_OPPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_OPPOSITION) },
                                role = com.aidebate.presentation.theme.DebateRole.CON,
                                label = t.argueAgainst,
                                subtitle = t.argueAgainst,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Live preview panel (User vs AI)
                        LivePreviewPanel(
                            mode = t.userVsAi,
                            userSide = uiState.userSide,
                            propositionProvider = uiState.providerProposition,
                            oppositionProvider = uiState.providerOpposition,
                        )
                    }
                }

                // AI Providers (hidden for F2F)
                AnimatedVisibility(visible = uiState.selectedMode != DebateMode.USER_VS_USER) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        ProviderSelector(
                            label = t.aiForProposition,
                            providers = uiState.enabledProviders,
                            selectedProvider = uiState.providerProposition,
                            selectedModel = uiState.modelProposition,
                            onProviderSelected = { viewModel.onProviderSelected("proposition", it) },
                            onModelChanged = { viewModel.onModelChanged("proposition", it) }
                        )

                        ProviderSelector(
                            label = t.aiForOpposition,
                            providers = uiState.enabledProviders,
                            selectedProvider = uiState.providerOpposition,
                            selectedModel = uiState.modelOpposition,
                            onProviderSelected = { viewModel.onProviderSelected("opposition", it) },
                            onModelChanged = { viewModel.onModelChanged("opposition", it) }
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // Start button
                Button(
                    onClick = { viewModel.startDebate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.canStart,
                    shape = Radii.mediumShape,
                    colors = glassButtonColors()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        if (uiState.selectedMode == DebateMode.USER_VS_USER) t.faceToFaceLabel else t.startDebate,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(Modifier.height(Spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun TopicCard(title: String) {
    val t = LocalTranslation.current
    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                t.topic,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LivePreviewPanel(
    mode: String,
    userSide: SpeakerRole?,
    propositionProvider: AiProvider?,
    oppositionProvider: AiProvider?,
) {
    val t = LocalTranslation.current
    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Tertiary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                t.preview,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User side
                val userTokens = if (userSide == SpeakerRole.AI_PROPOSITION)
                    RoleTokenDefaults.Pro else RoleTokenDefaults.Con

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(userTokens.color.container)
                    ) {
                        Icon(
                            Icons.Default.Person, null,
                            tint = userTokens.color.onContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t.you, style = MaterialTheme.typography.labelSmall,
                        color = userTokens.color.primary)
                    if (userSide != null) {
                        Text(userTokens.label, style = MaterialTheme.typography.labelSmall,
                            color = userTokens.color.primary.copy(alpha = 0.6f))
                    }
                }

                Text(t.vs, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))

                // AI side
                val aiTokens = RoleTokenDefaults.Con
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(aiTokens.color.container)
                    ) {
                        Icon(
                            Icons.Default.Adb, null,
                            tint = aiTokens.color.onContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(t.aiLabel, style = MaterialTheme.typography.labelSmall,
                        color = aiTokens.color.primary)
                    Text(
                        oppositionProvider?.displayName ?: t.aiLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = aiTokens.color.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun ModeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    GlassCard(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        accent = Primary
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    icon, contentDescription = null,
                    tint = if (selected) Primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ProviderSelector(
    label: String,
    providers: List<ProviderConfig>,
    selectedProvider: AiProvider?,
    selectedModel: String,
    onProviderSelected: (AiProvider) -> Unit,
    onModelChanged: (String) -> Unit
) {
    val t = LocalTranslation.current
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.sm))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedProvider?.displayName ?: t.selectProvider,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = Radii.smallShape,
                    colors = glassTextFieldColors()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    providers.forEach { config ->
                        DropdownMenuItem(
                            text = { Text("${config.provider.displayName} — ${config.modelName.ifBlank { t.defaultModel }}") },
                            onClick = {
                                onProviderSelected(config.provider)
                                expanded = false
                            }
                        )
                    }
                    if (providers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(t.noProvidersConfigured) },
                            onClick = { expanded = false }
                        )
                    }
                }
            }

            if (selectedProvider != null) {
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = onModelChanged,
                    label = { Text(t.modelName) },
                    placeholder = { Text(selectedProvider.defaultBaseUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = Radii.smallShape,
                    colors = glassTextFieldColors()
                )
            }
        }
    }
}
