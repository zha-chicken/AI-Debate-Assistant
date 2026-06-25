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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.presentation.localization.LocalTranslation
import com.aidebate.presentation.theme.*

@Composable
fun TopicSelectionScreen(
    onTopicSelected: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TopicSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val t = LocalTranslation.current

    LaunchedEffect(Unit) { viewModel.loadTopics() }

    var showCustomDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.customTopicResult) {
        uiState.customTopicResult?.let { topicId ->
            onTopicSelected(topicId)
            viewModel.clearCustomTopicResult()
        }
    }

    AiBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(t.chooseTopic, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, t.back)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCustomDialog = true }) {
                            Icon(Icons.Default.Add, t.customTopic)
                        }
                    },
                    colors = glassTopAppBarColors()
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
                        placeholder = { Text(t.searchTopics) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, t.clear)
                                }
                            }
                        },
                        singleLine = true,
                        shape = Radii.mediumShape,
                        colors = glassTextFieldColors()
                    )
                    Spacer(Modifier.height(Spacing.sm))
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
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(t.allTopicsFilter) },
                            colors = glassFilterChipColors(),
                            border = glassFilterChipBorder(selectedCategory == null)
                        )
                        uiState.categories.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                leadingIcon = {
                                    Icon(Icons.Default.Topic, null, Modifier.size(16.dp))
                                },
                                colors = glassFilterChipColors(),
                                border = glassFilterChipBorder(selectedCategory == category)
                            )
                        }
                    }
                }
            }

            // Topic list by category
            uiState.categories
                .filter { selectedCategory == null || it == selectedCategory }
                .forEach { category ->
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
                        TopicCard(
                            topic = topic,
                            usageCount = uiState.topicUsageCounts[topic.id] ?: 0,
                            onClick = { onTopicSelected(topic.id) }
                        )
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
                        Text(String.format(t.noTopicsMatch, searchQuery),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
    }

    uiState.customTopicError?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearCustomTopicError() },
            title = { Text(t.appTitle) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCustomTopicError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Custom topic dialog
    if (showCustomDialog) {
        var inputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false; inputText = "" },
            title = { Text(t.customTopicDialogTitle) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text(t.yourDebateQuestion) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = Radii.mediumShape,
                    colors = glassTextFieldColors()
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
                ) { Text(t.add) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false; inputText = "" }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun glassFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = GlassSurface,
    selectedContainerColor = GlassSurfaceStrong,
    labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
    iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    selectedLeadingIconColor = Primary
)

@Composable
private fun glassFilterChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = GlassStroke,
    selectedBorderColor = Primary.copy(alpha = 0.8f)
)

@Composable
private fun TopicCard(
    topic: com.aidebate.domain.model.DebateTopic,
    usageCount: Int,
    onClick: () -> Unit
) {
    val t = LocalTranslation.current
    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        accent = MaterialTheme.colorScheme.primary
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
                Text(
                    "${topic.category} · $usageCount ${t.debateBadge.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}
