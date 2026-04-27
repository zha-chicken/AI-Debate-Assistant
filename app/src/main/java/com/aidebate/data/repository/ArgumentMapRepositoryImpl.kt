package com.aidebate.data.repository

import com.aidebate.data.local.dao.ArgumentEdgeDao
import com.aidebate.data.local.dao.ArgumentNodeDao
import com.aidebate.data.local.entity.ArgumentEdgeEntity
import com.aidebate.data.local.entity.ArgumentNodeEntity
import com.aidebate.data.remote.adapter.AiProviderAdapter
import com.aidebate.data.remote.adapter.ProviderAdapterFactory
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.ArgumentMapRepository
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.TopicRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArgumentMapRepositoryImpl @Inject constructor(
    private val nodeDao: ArgumentNodeDao,
    private val edgeDao: ArgumentEdgeDao,
    private val topicRepository: TopicRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val adapterFactory: ProviderAdapterFactory
) : ArgumentMapRepository {

    override fun getNodes(topicId: String): Flow<List<ArgumentNode>> =
        nodeDao.getByTopicId(topicId).map { list -> list.map { it.toDomain() } }

    override fun getEdges(topicId: String): Flow<List<ArgumentEdge>> =
        edgeDao.getByTopicId(topicId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveNode(node: ArgumentNode) {
        nodeDao.insert(node.toEntity())
    }

    override suspend fun updateNode(node: ArgumentNode) {
        nodeDao.update(node.toEntity())
    }

    override suspend fun deleteNode(nodeId: String) {
        nodeDao.deleteById(nodeId)
    }

    override suspend fun saveEdge(edge: ArgumentEdge) {
        edgeDao.insert(edge.toEntity())
    }

    override suspend fun deleteEdge(edgeId: String) {
        edgeDao.deleteById(edgeId)
    }

    override suspend fun deleteAllForTopic(topicId: String) {
        edgeDao.deleteByTopicId(topicId)
        nodeDao.deleteByTopicId(topicId)
    }

    suspend fun generateArgumentMap(topicId: String): List<ArgumentNode> {
        val topic = topicRepository.getTopic(topicId) ?: throw Exception("Topic not found")
        val configs = providerConfigRepository.getEnabledConfigs().first()
        val config = configs.firstOrNull() ?: throw Exception("No AI provider configured")
        val adapter = adapterFactory.getAdapter(config.provider)

        val prompt = buildString {
            append("You are helping a debater prepare. Create an argument map for the topic: \"${topic.title}\".\n")
            append("Return a JSON array of arguments. Each argument object has these fields:\n")
            append("- title: short argument title (max 8 words)\n")
            append("- type: \"PRO\" or \"CON\"\n")
            append("- content: 1-2 sentence explanation\n")
            append("Include 3 PRO arguments and 3 CON arguments. Return ONLY valid JSON, no other text.\n")
            append("Example: [{\"title\":\"Example Pro\",\"type\":\"PRO\",\"content\":\"This is why...\"}]")
        }

        val result = adapter.chat(
            systemPrompt = "You are a debate preparation assistant. Always return valid JSON.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = ChatConfig(model = config.modelName.ifBlank { "gpt-4o" }),
            providerConfig = config
        )

        return parseArgumentJson(result.content, topicId)
    }

    private fun parseArgumentJson(json: String, topicId: String): List<ArgumentNode> {
        val cleanedJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
        val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
        val parsed = adapter.fromJson(cleanedJson) ?: return emptyList()

        val nodes = mutableListOf<ArgumentNode>()
        val centerX = 0f; val centerY = 0f
        val proX = 250f; val conX = -250f
        val startY = -160f; val yGap = 140f

        parsed.forEachIndexed { index, map ->
            val title = map["title"]?.toString() ?: "Argument ${index + 1}"
            val type = when (map["type"]?.toString()?.uppercase()) {
                "PRO" -> NodeType.PRO
                "CON" -> NodeType.CON
                else -> NodeType.PRO
            }
            val content = map["content"]?.toString() ?: ""

            val proCount = nodes.count { it.type == NodeType.PRO }
            val conCount = nodes.count { it.type == NodeType.CON }

            val x = if (type == NodeType.PRO) proX else conX
            val y = if (type == NodeType.PRO) {
                startY + proCount * yGap
            } else {
                startY + conCount * yGap
            }

            nodes.add(ArgumentNode(
                topicId = topicId, type = type, title = title,
                content = content, xPosition = x, yPosition = y
            ))
        }
        return nodes
    }
}

private fun ArgumentNode.toEntity() = ArgumentNodeEntity(
    id = id, topicId = topicId, type = type.name, title = title,
    content = content, parentId = parentId, xPosition = xPosition,
    yPosition = yPosition, createdAt = createdAt
)

private fun ArgumentNodeEntity.toDomain() = ArgumentNode(
    id = id, topicId = topicId, type = NodeType.valueOf(type),
    title = title, content = content, parentId = parentId,
    xPosition = xPosition, yPosition = yPosition, createdAt = createdAt
)

private fun ArgumentEdge.toEntity() = ArgumentEdgeEntity(
    id = id, topicId = topicId, fromNodeId = fromNodeId,
    toNodeId = toNodeId, relation = relation.name
)

private fun ArgumentEdgeEntity.toDomain() = ArgumentEdge(
    id = id, topicId = topicId, fromNodeId = fromNodeId,
    toNodeId = toNodeId, relation = EdgeRelation.valueOf(relation)
)
