@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.setup

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.DebateFormat
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.model.SpeakerRole

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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Topic info
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Topic",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            uiState.topicTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Debate Mode
                Text("Debate Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text("Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                // User side (only for User vs AI)
                AnimatedVisibility(visible = uiState.selectedMode == DebateMode.USER_VS_AI) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Your Position",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SideCard(
                                title = "For",
                                description = "Argue in favor",
                                icon = Icons.Default.ThumbUp,
                                selected = uiState.userSide == SpeakerRole.AI_PROPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_PROPOSITION) },
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            SideCard(
                                title = "Against",
                                description = "Argue against",
                                icon = Icons.Default.ThumbDown,
                                selected = uiState.userSide == SpeakerRole.AI_OPPOSITION,
                                onClick = { viewModel.onUserSideSelected(SpeakerRole.AI_OPPOSITION) },
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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

                Spacer(Modifier.height(8.dp))

                // Start button
                Button(
                    onClick = { viewModel.startDebate() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState.canStart,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Debate", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
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
        shape = RoundedCornerShape(12.dp),
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
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
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
private fun SideCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(color)
        ) else null
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (selected) color else color.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            // Provider dropdown
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedProvider?.displayName ?: "Select provider",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
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

            // Model input
            if (selectedProvider != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = selectedModel,
                    onValueChange = onModelChanged,
                    label = { Text("Model name") },
                    placeholder = { Text(selectedProvider.defaultBaseUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}
