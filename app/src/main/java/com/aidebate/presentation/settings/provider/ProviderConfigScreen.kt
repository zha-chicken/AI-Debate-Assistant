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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*

@Composable
fun ProviderConfigScreen(
    providerName: String,
    onBack: () -> Unit,
    viewModel: ProviderConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current
    var apiKeyVisible by remember { mutableStateOf(false) }

    LaunchedEffect(providerName) { viewModel.initialize(providerName) }

    val saved by viewModel.saved.collectAsState()
    LaunchedEffect(saved) {
        if (saved) onBack()
    }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(uiState.providerName) },
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), accent = Primary) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(t.enableProvider, fontWeight = FontWeight.SemiBold)
                                Text(t.enableProviderSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = { viewModel.onEnabledChanged(it) }
                            )
                        }
                    }

                    Text(t.apiKey, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = { viewModel.onApiKeyChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(t.apiKey) },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    if (apiKeyVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    t.toggleVisibility
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors()
                    )

                    Text(t.model, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = uiState.modelName,
                        onValueChange = { viewModel.onModelChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(t.modelName) },
                        placeholder = { Text(getDefaultModel(providerName)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = glassTextFieldColors()
                    )

                    AnimatedVisibility(visible = uiState.showBaseUrl) {
                        Column {
                            Text(t.baseUrl, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = uiState.baseUrl,
                                onValueChange = { viewModel.onBaseUrlChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(t.baseUrl) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = glassTextFieldColors()
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.saveConfig() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !uiState.isSaving,
                        colors = glassButtonColors()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(t.saveConfiguration, style = MaterialTheme.typography.titleMedium)
                        }
                    }

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
                            Text(t.testConnection)
                        }
                    }

                    when (uiState.validationState) {
                        is ProviderConfigUiState.ValidationState.Valid -> {
                            GlassCard(accent = Tertiary) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle,
                                        null, tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(t.connectionSuccessful,
                                        color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        is ProviderConfigUiState.ValidationState.Error -> {
                            val msg = (uiState.validationState as ProviderConfigUiState.ValidationState.Error).message
                            GlassCard(accent = MaterialTheme.colorScheme.error) {
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
