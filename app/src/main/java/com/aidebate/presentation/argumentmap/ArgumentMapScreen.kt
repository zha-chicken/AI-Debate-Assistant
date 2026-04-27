@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aidebate.presentation.argumentmap

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aidebate.domain.model.NodeType
import com.aidebate.presentation.theme.SuccessGreen
import com.aidebate.presentation.theme.WarningAmber
import kotlin.math.sqrt

@Composable
fun ArgumentMapScreen(
    topicId: String,
    onBack: () -> Unit,
    viewModel: ArgumentMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(topicId) { viewModel.initialize(topicId) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.25f, 3.5f)
        offset += panChange
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.topicTitle.ifBlank { "Argument Map" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, maxLines = 1)
                        if (uiState.nodes.isNotEmpty()) {
                            Text("${uiState.nodes.size} arguments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.nodes.isNotEmpty()) {
                        IconButton(onClick = { viewModel.generateMap() },
                            enabled = !uiState.isGenerating) {
                            if (uiState.isGenerating)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            else Icon(Icons.Filled.AutoAwesome, "Regenerate")
                        }
                    }
                    MenuBox(
                        onAddPro = { viewModel.showAddDialog(NodeType.PRO) },
                        onAddCon = { viewModel.showAddDialog(NodeType.CON) },
                        onAddEvidence = { viewModel.showAddDialog(NodeType.EVIDENCE) }
                    )
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedNodeId != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        uiState.nodes.find { it.id == uiState.selectedNodeId }?.let {
                            viewModel.showEditDialog(it.id)
                        }
                    },
                    icon = { Icon(Icons.Default.Edit, "Edit") },
                    text = { Text("Edit") }
                )
            }
        }
    ) { padding ->
        // Node colors resolved once from theme
        val proColor = SuccessGreen
        val conColor = MaterialTheme.colorScheme.error
        val evidenceColor = WarningAmber
        val topicColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.nodes.isEmpty() && !uiState.isGenerating) {
                EmptyState(onGenerate = { viewModel.generateMap() })
            } else {
                val edgeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .transformable(transformState)
                        .pointerInput(uiState.nodes) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val hitRadius = 46f * scale
                                val hitNode = uiState.nodes.find { node ->
                                    val nx = center.x + node.xPosition * scale + offset.x
                                    val ny = center.y + node.yPosition * scale + offset.y
                                    val dist = sqrt(
                                        (tapOffset.x - nx) * (tapOffset.x - nx) +
                                        (tapOffset.y - ny) * (tapOffset.y - ny)
                                    )
                                    dist < hitRadius
                                }
                                viewModel.onNodeSelected(hitNode?.id)
                            }
                        }
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Draw curved edges
                    uiState.edges.forEach { edge ->
                        val fromNode = uiState.nodes.find { it.id == edge.fromNodeId }
                        val toNode = uiState.nodes.find { it.id == edge.toNodeId }
                        if (fromNode != null && toNode != null) {
                            val from = Offset(
                                center.x + fromNode.xPosition * scale,
                                center.y + fromNode.yPosition * scale
                            )
                            val to = Offset(
                                center.x + toNode.xPosition * scale,
                                center.y + toNode.yPosition * scale
                            )
                            // Quadratic bezier for curved edges
                            val midX = (from.x + to.x) / 2f
                            val midY = (from.y + to.y) / 2f
                            val control = Offset(midX, midY - 40f * scale)

                            val path = Path().apply {
                                moveTo(from.x, from.y)
                                quadraticBezierTo(control.x, control.y, to.x, to.y)
                            }
                            drawPath(
                                path = path,
                                color = edgeColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2f * scale.coerceIn(0.5f, 2f)
                                )
                            )
                        }
                    }

                    // Topic center node
                    drawNode(
                        title = uiState.topicTitle.ifBlank { "Topic" },
                        x = center.x, y = center.y,
                        color = topicColor,
                        isSelected = false,
                        scale = scale
                    )

                    // Argument nodes
                    uiState.nodes.forEach { node ->
                        val nodeColor = when (node.type) {
                            NodeType.PRO -> proColor
                            NodeType.CON -> conColor
                            NodeType.EVIDENCE -> evidenceColor
                            NodeType.TOPIC -> topicColor
                        }
                        drawNode(
                            title = node.title,
                            x = center.x + node.xPosition * scale,
                            y = center.y + node.yPosition * scale,
                            color = nodeColor,
                            isSelected = node.id == uiState.selectedNodeId,
                            scale = scale
                        )
                    }
                }
            }

            // Loading overlay
            AnimatedVisibility(
                visible = uiState.isGenerating,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Generating argument map...",
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Error
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(uiState.error ?: "", Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Dismiss")
                        }
                    }
                }
            }
        }

        // Add/Edit dialogs
        if (uiState.showAddDialog || uiState.showEditDialog) {
            NodeEditDialog(uiState = uiState, viewModel = viewModel)
        }
    }
}

@Composable
private fun EmptyState(onGenerate: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "empty").animateFloat(
        0.85f, 1f, infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse), label = "p"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.AccountTree, null,
            Modifier.size(72.dp).scale(pulse),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Spacer(Modifier.height(16.dp))
        Text("No argument map yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text("Generate a visual mindmap using AI",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGenerate, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate with AI")
        }
    }
}

private fun DrawScope.drawNode(
    title: String, x: Float, y: Float,
    color: Color, isSelected: Boolean, scale: Float
) {
    val baseRadius = 36f
    val radius = if (isSelected) baseRadius * 1.15f else baseRadius
    val iconSize = radius * scale.coerceIn(0.7f, 1.3f)

    // Shadow ring for selected
    if (isSelected) {
        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = radius * scale + 8f,
            center = Offset(x, y)
        )
    }

    // Outer circle
    drawCircle(
        color = color.copy(alpha = 0.92f),
        radius = radius * scale,
        center = Offset(x, y)
    )
    // Inner highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.25f),
        radius = (radius - 5f) * scale,
        center = Offset(x, y)
    )

    // Title text
    val displayTitle = if (title.length > 14) title.take(13) + "…" else title
    val textPaint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = (24f * scale.coerceIn(0.6f, 1.5f))
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = android.graphics.Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText(displayTitle, x, y + 8f * scale, textPaint)
}

@Composable
private fun NodeEditDialog(
    uiState: ArgumentMapUiState,
    viewModel: ArgumentMapViewModel
) {
    var title by remember(uiState.showAddDialog, uiState.showEditDialog) {
        mutableStateOf(uiState.editNodeTitle)
    }
    var content by remember(uiState.showAddDialog, uiState.showEditDialog) {
        mutableStateOf(uiState.editNodeContent)
    }
    var nodeType by remember(uiState.showAddDialog, uiState.showEditDialog) {
        mutableStateOf(uiState.editNodeType)
    }

    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                if (uiState.showAddDialog) "Add Argument" else "Edit Argument",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        NodeType.PRO to "Pro",
                        NodeType.CON to "Con",
                        NodeType.EVIDENCE to "Evidence"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = nodeType == type,
                            onClick = { nodeType = type },
                            label = {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            },
                            leadingIcon = {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape).background(
                                        when (type) {
                                            NodeType.PRO -> SuccessGreen
                                            NodeType.CON -> MaterialTheme.colorScheme.error
                                            NodeType.EVIDENCE -> WarningAmber
                                            NodeType.TOPIC -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Details") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.onEditTitleChanged(title)
                viewModel.onEditContentChanged(content)
                viewModel.onEditTypeChanged(nodeType)
                if (uiState.showAddDialog) viewModel.saveNewNode()
                else viewModel.saveEditedNode()
            }) { Text("Save", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            Row {
                if (uiState.showEditDialog && uiState.selectedNodeId != null) {
                    TextButton(onClick = { viewModel.deleteSelectedNode() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun MenuBox(
    onAddPro: () -> Unit,
    onAddCon: () -> Unit,
    onAddEvidence: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, "Add")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Pro Argument") },
                leadingIcon = {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(SuccessGreen))
                },
                onClick = { expanded = false; onAddPro() }
            )
            DropdownMenuItem(
                text = { Text("Con Argument") },
                leadingIcon = {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                },
                onClick = { expanded = false; onAddCon() }
            )
            DropdownMenuItem(
                text = { Text("Evidence") },
                leadingIcon = {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(WarningAmber))
                },
                onClick = { expanded = false; onAddEvidence() }
            )
        }
    }
}
