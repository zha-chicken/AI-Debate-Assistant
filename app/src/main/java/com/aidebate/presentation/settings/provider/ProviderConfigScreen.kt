@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.settings.provider

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProviderConfigScreen(
    providerName: String,
    onBack: () -> Unit,
    viewModel: ProviderConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var apiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(providerName) { viewModel.initialize(providerName) }

    val saved by viewModel.saved.collectAsState()
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.providerName) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enabled toggle
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Provider", fontWeight = FontWeight.SemiBold)
                            Text("Turn on to use this AI in debates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = { viewModel.onEnabledChanged(it) }
                        )
                    }
                }

                // API Key
                Text("API Key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.onApiKeyChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                if (apiKeyVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                "Toggle visibility"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Model name
                Text("Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = uiState.modelName,
                    onValueChange = { viewModel.onModelChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model name") },
                    placeholder = { Text(getDefaultModel(providerName)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Base URL (shown for all providers)
                AnimatedVisibility(visible = uiState.showBaseUrl) {
                    Column {
                        Text("Base URL", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = uiState.baseUrl,
                            onValueChange = { viewModel.onBaseUrlChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Base URL") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Save button
                Button(
                    onClick = { viewModel.saveConfig() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Configuration", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Validate button
                OutlinedButton(
                    onClick = { viewModel.validateConfig() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.validationState == ProviderConfigUiState.ValidationState.Validating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.NetworkCheck, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test Connection")
                    }
                }

                // Validation result
                when (uiState.validationState) {
                    is ProviderConfigUiState.ValidationState.Valid -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle,
                                    null, tint = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.width(8.dp))
                                Text("Connection successful!",
                                    color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    is ProviderConfigUiState.ValidationState.Error -> {
                        val msg = (uiState.validationState as ProviderConfigUiState.ValidationState.Error).message
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline,
                                    null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(msg, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    ProviderConfigUiState.ValidationState.Idle -> {}
                    ProviderConfigUiState.ValidationState.Validating -> {}
                }
            }
        }
    }
}

private fun getDefaultModel(providerName: String): String = when (providerName) {
    "OPENAI" -> "gpt-4o"
    "ANTHROPIC" -> "claude-sonnet-4-20250514"
    "GEMINI" -> "gemini-1.5-flash"
    "DEEPSEEK" -> "deepseek-chat"
    "GROQ" -> "llama3-70b-8192"
    "OLLAMA" -> "llama3.2"
    else -> ""
}
