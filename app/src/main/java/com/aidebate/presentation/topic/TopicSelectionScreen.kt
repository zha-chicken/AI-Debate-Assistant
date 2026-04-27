@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.topic

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.theme.*

@Composable
fun TopicSelectionScreen(
    onTopicSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TopicSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadTopics() }

    var showCustomDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Topic") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, "Custom topic")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Search bar
            item(key = "search") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search topics...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = Radii.mediumShape
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            // Custom topic shortcut
            item(key = "customTopic") {
                OutlinedCard(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Radii.mediumShape,
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.primary
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            Text("Write your own topic",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Enter a custom debate question",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Category chips
            if (uiState.categories.isNotEmpty()) {
                item(key = "chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        uiState.categories.forEach { category ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text(category) },
                                leadingIcon = {
                                    Icon(Icons.Default.Topic, null, Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }

            // Topic list by category
            uiState.categories.forEach { category ->
                val categoryTopics = uiState.topicsByCategory[category] ?: emptyList()
                val filtered = if (searchQuery.isBlank()) categoryTopics
                else categoryTopics.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                }

                if (filtered.isNotEmpty()) {
                    item(key = "header_$category") {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
                        )
                    }

                    items(filtered, key = { it.id }) { topic ->
                        TopicCard(topic = topic, onClick = { onTopicSelected(topic.id) })
                    }
                }
            }

            // Empty search state
            if (searchQuery.isNotBlank() && uiState.categories.all { cat ->
                    val topics = uiState.topicsByCategory[cat] ?: emptyList()
                    topics.none { it.title.contains(searchQuery, ignoreCase = true) }
                }) {
                item(key = "empty") {
                    Box(
                        Modifier.fillMaxWidth().padding(Spacing.xxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No topics match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    // Custom topic dialog
    if (showCustomDialog) {
        var inputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false; inputText = "" },
            title = { Text("Custom Topic") },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Your debate question") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = Radii.mediumShape
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.addCustomTopic(inputText.trim())
                            showCustomDialog = false
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false; inputText = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TopicCard(
    topic: com.aidebate.domain.model.DebateTopic,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.mediumShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline, null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(topic.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (topic.description.isNotBlank()) {
                    Text(topic.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
