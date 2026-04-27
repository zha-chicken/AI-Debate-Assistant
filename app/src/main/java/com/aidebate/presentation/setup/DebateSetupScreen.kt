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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.DebateFormat
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.presentation.common.RolePill
import com.aidebate.presentation.common.RoleSelectionCard
import com.aidebate.presentation.theme.*

@Composable
fun DebateSetupScreen(
    topicId: String,
    onStartDebate: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: DebateSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(topicId) { viewModel.initialize(topicId) }

    val sessionId = viewModel.sessionId.collectAsState()

    LaunchedEffect(sessionId.value) {
        sessionId.value?.let { onStartDebate(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debate Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
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
                SectionLabel("Debate Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ModeCard(
                        title = "User vs AI",
                        icon = Icons.Default.Person,
                        selected = uiState.selectedMode == DebateMode.USER_VS_AI,
                        onClick = { viewModel.onModeSelected(DebateMode.USER_VS_AI) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeCard(
                        title = "AI vs AI",
                        icon = Icons.Default.Adb,
                        selected = uiState.selectedMode == DebateMode.AI_VS_AI,
                        onClick = { viewModel.onModeSelected(DebateMode.AI_VS_AI) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Debate Format
                SectionLabel("Format")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    ModeCard(
                        title = "Structured",
                        subtitle = "Opening, Rebuttal, Closing",
                        icon = Icons.Default.Segment,
                        selected = uiState.selectedFormat == DebateFormat.STRUCTURED,
                        onClick = { viewModel.onFormatSelected(DebateFormat.STRUCTURED) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeCard(
                        title = "Free Flow",
                        subtitle = "Open conversation",
                        icon = Icons.Default.Chat,
                        selected = uiState.selectedFormat == DebateFormat.FREE_FLOW,
                        onClick = { viewModel.onFormatSelected(DebateFormat.FREE_FLOW) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // User position (only for User vs AI)
                AnimatedVisibility(visible = uiState.selectedMode == DebateMode.USER_VS_AI) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        SectionLabel("Your Position")
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            RoleSelectionCard(
                                selected = uiState.userSide == SpeakerRole.AI_PROPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_PROPOSITION) },
                                role = com.aidebate.presentation.theme.DebateRole.PRO,
                                label = "For",
                                subtitle = "Argue in favor",
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectionCard(
                                selected = uiState.userSide == SpeakerRole.AI_OPPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_OPPOSITION) },
                                role = com.aidebate.presentation.theme.DebateRole.CON,
                                label = "Against",
                                subtitle = "Argue against",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Live preview panel (User vs AI)
                        LivePreviewPanel(
                            mode = "User vs AI",
                            userSide = uiState.userSide,
                            propositionProvider = uiState.providerProposition,
                            oppositionProvider = uiState.providerOpposition,
                        )
                    }
                }

                // AI Provider for Proposition
                ProviderSelector(
                    label = "AI for Proposition",
                    providers = uiState.enabledProviders,
                    selectedProvider = uiState.providerProposition,
                    selectedModel = uiState.modelProposition,
                    onProviderSelected = { viewModel.onProviderSelected("proposition", it) },
                    onModelChanged = { viewModel.onModelChanged("proposition", it) }
                )

                // AI Provider for Opposition
                ProviderSelector(
                    label = "AI for Opposition",
                    providers = uiState.enabledProviders,
                    selectedProvider = uiState.providerOpposition,
                    selectedModel = uiState.modelOpposition,
                    onProviderSelected = { viewModel.onProviderSelected("opposition", it) },
                    onModelChanged = { viewModel.onModelChanged("opposition", it) }
                )

                Spacer(Modifier.height(Spacing.sm))

                // Start button
                Button(
                    onClick = { viewModel.startDebate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.canStart,
                    shape = Radii.mediumShape
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Start Debate", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun TopicCard(title: String) {
    Card(
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                "Topic",
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
    Card(
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
        ) {
            Text(
                "Preview",
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
                    Text("You", style = MaterialTheme.typography.labelSmall,
                        color = userTokens.color.primary)
                    if (userSide != null) {
                        Text(userTokens.label, style = MaterialTheme.typography.labelSmall,
                            color = userTokens.color.primary.copy(alpha = 0.6f))
                    }
                }

                Text("vs", style = MaterialTheme.typography.titleMedium,
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
                    Text("AI", style = MaterialTheme.typography.labelSmall,
                        color = aiTokens.color.primary)
                    Text(
                        oppositionProvider?.displayName ?: "AI",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(Spacing.sm))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedProvider?.displayName ?: "Select provider",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = Radii.smallShape
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    providers.forEach { config ->
                        DropdownMenuItem(
                            text = { Text("${config.provider.displayName} — ${config.modelName.ifBlank { "default" }}") },
                            onClick = {
                                onProviderSelected(config.provider)
                                expanded = false
                            }
                        )
                    }
                    if (providers.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No providers configured. Go to Settings.") },
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
                    label = { Text("Model name") },
                    placeholder = { Text(selectedProvider.defaultBaseUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = Radii.smallShape
                )
            }
        }
    }
}
