package com.aidebate.presentation.argumentmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.ArgumentMapRepository
import com.aidebate.domain.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArgumentMapUiState(
    val topicTitle: String = "",
    val topicId: String = "",
    val nodes: List<ArgumentNode> = emptyList(),
    val edges: List<ArgumentEdge> = emptyList(),
    val selectedNodeId: String? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editNodeTitle: String = "",
    val editNodeContent: String = "",
    val editNodeType: NodeType = NodeType.PRO
)

@HiltViewModel
class ArgumentMapViewModel @Inject constructor(
    private val repository: ArgumentMapRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArgumentMapUiState())
    val uiState: StateFlow<ArgumentMapUiState> = _uiState.asStateFlow()

    fun initialize(topicId: String) {
        _uiState.update { it.copy(topicId = topicId) }
        viewModelScope.launch {
            val topic = topicRepository.getTopic(topicId)
            _uiState.update { it.copy(topicTitle = topic?.title ?: "") }
        }
        viewModelScope.launch {
            repository.getNodes(topicId).collect { nodes ->
                _uiState.update { it.copy(nodes = nodes) }
            }
        }
        viewModelScope.launch {
            repository.getEdges(topicId).collect { edges ->
                _uiState.update { it.copy(edges = edges) }
            }
        }
    }

    fun generateMap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                val impl = repository as com.aidebate.data.repository.ArgumentMapRepositoryImpl
                repository.deleteAllForTopic(_uiState.value.topicId)
                val generatedNodes = impl.generateArgumentMap(_uiState.value.topicId)
                generatedNodes.forEach { repository.saveNode(it) }
                _uiState.update { it.copy(isGenerating = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false,
                        error = e.message ?: "Failed to generate map")
                }
            }
        }
    }

    fun onNodeMoved(nodeId: String, newX: Float, newY: Float) {
        val node = _uiState.value.nodes.find { it.id == nodeId } ?: return
        val updated = node.copy(xPosition = newX, yPosition = newY)
        _uiState.update { state ->
            state.copy(nodes = state.nodes.map { if (it.id == nodeId) updated else it })
        }
        viewModelScope.launch { repository.updateNode(updated) }
    }

    fun onNodeSelected(nodeId: String?) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun showAddDialog(type: NodeType) {
        _uiState.update {
            it.copy(showAddDialog = true, editNodeType = type,
                editNodeTitle = "", editNodeContent = "")
        }
    }

    fun showEditDialog(nodeId: String) {
        val node = _uiState.value.nodes.find { it.id == nodeId } ?: return
        _uiState.update {
            it.copy(showEditDialog = true, selectedNodeId = nodeId,
                editNodeTitle = node.title, editNodeContent = node.content,
                editNodeType = node.type)
        }
    }

    fun onEditTitleChanged(title: String) {
        _uiState.update { it.copy(editNodeTitle = title) }
    }

    fun onEditContentChanged(content: String) {
        _uiState.update { it.copy(editNodeContent = content) }
    }

    fun onEditTypeChanged(type: NodeType) {
        _uiState.update { it.copy(editNodeType = type) }
    }

    fun saveNewNode() {
        val state = _uiState.value
        if (state.editNodeTitle.isBlank()) return
        val node = ArgumentNode(
            topicId = state.topicId, type = state.editNodeType,
            title = state.editNodeTitle, content = state.editNodeContent,
            xPosition = 100f, yPosition = (state.nodes.size * 120f) - 200f
        )
        viewModelScope.launch { repository.saveNode(node) }
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun saveEditedNode() {
        val state = _uiState.value
        val nodeId = state.selectedNodeId ?: return
        val node = state.nodes.find { it.id == nodeId } ?: return
        val updated = node.copy(
            title = state.editNodeTitle, content = state.editNodeContent,
            type = state.editNodeType
        )
        viewModelScope.launch { repository.updateNode(updated) }
        _uiState.update { it.copy(showEditDialog = false) }
    }

    fun deleteSelectedNode() {
        val nodeId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch { repository.deleteNode(nodeId) }
        _uiState.update { it.copy(selectedNodeId = null, showEditDialog = false) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, showEditDialog = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
