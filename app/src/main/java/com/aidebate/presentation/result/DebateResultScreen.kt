@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.result

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.SpeakerRole

@Composable
fun DebateResultScreen(
    sessionId: String,
    onBackToHome: () -> Unit,
    onBack: () -> Unit,
    viewModel: DebateResultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(sessionId) { viewModel.initialize(sessionId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debate Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            append("AI Debate Result\n\n")
                            append("Topic: ${uiState.topicTitle}\n\n")
                            uiState.turns.forEach { turn ->
                                append("${turn.speakerRole.name}: ${turn.content}\n\n")
                            }
                            if (uiState.result != null) {
                                append("WINNER: ${uiState.result!!.winner?.name}\n")
                                append(uiState.result!!.summary)
                            }
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Winner card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.height(12.dp))
                            Text("Topic: ${uiState.topicTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(8.dp))
                            if (uiState.result?.winner != null) {
                                Text("Winner",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                Text(
                                    uiState.result!!.winner!!.name.replace("AI_", ""),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                if (uiState.result!!.summary.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(uiState.result!!.summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center)
                                }
                            } else {
                                Text("No winner declared",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }

                // Transcript
                item {
                    Text("Transcript",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp))
                }

                items(uiState.turns) { turn ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                turn.speakerRole.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(turn.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Back to home button
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Home, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Back to Home")
                    }
                }
            }
        }
    }
}
