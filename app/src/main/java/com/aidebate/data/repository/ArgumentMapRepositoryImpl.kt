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

    suspend fun generateThreeRoundDebate(topicId: String): List<ArgumentMapDebateTurn> {
        val topic = topicRepository.getTopic(topicId) ?: throw Exception("Topic not found")
        val configs = providerConfigRepository.getEnabledConfigs().first()
        val config = configs.firstOrNull() ?: throw Exception("No AI provider configured")
        val aiAdapter = adapterFactory.getAdapter(config.provider)
        val chatConfig = ChatConfig(model = config.modelName.ifBlank { "gpt-4o" })

        val prompt = buildString {
            append("Run a concise 3-round AI vs AI debate for this topic: \"${topic.title}\".\n")
            append("Side PRO defends the proposition. Side CON challenges it.\n")
            append("Each round must include one PRO turn and one CON turn.\n")
            append("Each turn should be 45-70 words, specific, and contain extractable claims/evidence/objections.\n")
            append("Return ONLY valid JSON with this exact shape:\n")
            append("""{"turns":[{"round":1,"side":"PRO","content":"..."},{"round":1,"side":"CON","content":"..."}]}""")
        }

        val result = aiAdapter.chat(
            systemPrompt = "You are simulating a balanced academic debate. Return valid JSON only.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = chatConfig,
            providerConfig = config
        )

        return runCatching {
            parseDebateTurns(result.content)
        }.getOrElse {
            repairDebateJson(
                invalidJson = result.content,
                aiAdapter = aiAdapter,
                chatConfig = chatConfig,
                providerConfig = config
            ).let { repaired -> parseDebateTurns(repaired) }
        }
    }

    suspend fun generateArgumentMap(
        topicId: String,
        debateTurns: List<ArgumentMapDebateTurn> = emptyList()
    ): ArgumentGraph {
        val topic = topicRepository.getTopic(topicId) ?: throw Exception("Topic not found")
        val configs = providerConfigRepository.getEnabledConfigs().first()
        val config = configs.firstOrNull() ?: throw Exception("No AI provider configured")
        val aiAdapter = adapterFactory.getAdapter(config.provider)
        val chatConfig = ChatConfig(model = config.modelName.ifBlank { "gpt-4o" })
        val debateTranscript = debateTurns.joinToString("\n") {
            "Round ${it.round} ${it.side}: ${it.content}"
        }

        val prompt = buildString {
            append("You are helping a debater prepare. Create a detailed argument relationship graph for the topic: \"${topic.title}\".\n")
            if (debateTranscript.isNotBlank()) {
                append("Base the graph on this 3-round AI vs AI debate transcript. Extract concrete claims, objections, rebuttals, and evidence from it.\n\n")
                append("=== DEBATE TRANSCRIPT ===\n")
                append(debateTranscript)
                append("\n=== END TRANSCRIPT ===\n\n")
            }
            append("Return ONLY valid JSON with this exact shape:\n")
            append("{\"nodes\":[{\"localId\":\"pro1\",\"title\":\"short title\",\"type\":\"PRO\",\"content\":\"1-2 sentence explanation\"}],")
            append("\"edges\":[{\"from\":\"pro1\",\"to\":\"con1\",\"relation\":\"REFUTES\"}]}\n")
            append("Use exactly these node IDs: pro1, pro2, pro3, pro4, con1, con2, con3, con4, ev1, ev2, ev3, ev4.\n")
            append("Rules:\n")
            append("- Include 4 PRO nodes and 4 CON nodes.\n")
            append("- Include 4 EVIDENCE nodes that support or challenge the strongest claims.\n")
            append("- Node type must be \"PRO\", \"CON\", or \"EVIDENCE\".\n")
            append("- Edge relation must be \"SUPPORTS\", \"REFUTES\", or \"RELATES\".\n")
            append("- Every edge from/to must reference a localId from nodes.\n")
            append("- Include at least 12 edges showing support, refutation, and related reasoning.\n")
            append("- Every PRO and CON node must have at least one relationship.\n")
            append("- Titles must be max 8 words.\n")
            append("- Return no markdown and no commentary.")
        }

        val result = aiAdapter.chat(
            systemPrompt = "You are a debate preparation assistant. Always return valid JSON.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = chatConfig,
            providerConfig = config
        )

        return runCatching {
            parseArgumentJson(result.content, topicId)
        }.getOrElse {
            val repairedJson = repairArgumentJson(
                invalidJson = result.content,
                aiAdapter = aiAdapter,
                chatConfig = chatConfig,
                providerConfig = config
            )
            runCatching {
                parseArgumentJson(repairedJson, topicId)
            }.getOrElse { repairError ->
                throw Exception("AI returned invalid graph JSON. Please regenerate. ${repairError.message ?: ""}".trim())
            }
        }
    }

    private fun parseDebateTurns(json: String): List<ArgumentMapDebateTurn> {
        val cleanedJson = extractJsonObject(json)
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(mapType)
        val parsed = adapter.fromJson(cleanedJson) ?: throw IllegalArgumentException("Empty debate JSON")
        val rawTurns = parsed["turns"] as? List<*> ?: throw IllegalArgumentException("Missing debate turns")
        return rawTurns.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            ArgumentMapDebateTurn(
                round = (map["round"] as? Number)?.toInt() ?: map["round"]?.toString()?.toIntOrNull() ?: 1,
                side = map["side"]?.toString()?.uppercase()?.let {
                    if (it == "CON" || it == "OPPOSE" || it == "OPPOSITION") "CON" else "PRO"
                } ?: "PRO",
                content = map["content"]?.toString()?.trim().orEmpty()
            )
        }.filter { it.content.isNotBlank() }.take(6).ifEmpty {
            throw IllegalArgumentException("No usable debate turns")
        }
    }

    private fun parseArgumentJson(json: String, topicId: String): ArgumentGraph {
        val cleanedJson = extractJsonObject(json)

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(mapType)
        val parsed = adapter.fromJson(cleanedJson) ?: throw IllegalArgumentException("Empty graph JSON")
        val parsedNodes = parsed["nodes"] as? List<*> ?: throw IllegalArgumentException("Missing graph nodes")
        val parsedEdges = parsed["edges"] as? List<*> ?: emptyList<Any>()

        val nodes = mutableListOf<ArgumentNode>()
        val localIdToNodeId = mutableMapOf<String, String>()
        val proX = 300f
        val conX = -300f
        val evidenceX = 0f
        val startY = -260f
        val yGap = 150f

        parsedNodes.forEachIndexed { index, raw ->
            val map = raw as? Map<*, *> ?: return@forEachIndexed
            val localId = map["localId"]?.toString()
                ?: map["id"]?.toString()
                ?: "node_${index + 1}"
            val title = map["title"]?.toString() ?: "Argument ${index + 1}"
            val type = when (map["type"]?.toString()?.uppercase()) {
                "PRO" -> NodeType.PRO
                "CON" -> NodeType.CON
                "EVIDENCE" -> NodeType.EVIDENCE
                else -> NodeType.PRO
            }
            val content = map["content"]?.toString() ?: ""

            val proCount = nodes.count { it.type == NodeType.PRO }
            val conCount = nodes.count { it.type == NodeType.CON }
            val evidenceCount = nodes.count { it.type == NodeType.EVIDENCE }

            val x = when (type) {
                NodeType.PRO -> proX
                NodeType.CON -> conX
                NodeType.EVIDENCE -> evidenceX
                NodeType.TOPIC -> 0f
            }
            val y = when (type) {
                NodeType.PRO -> startY + proCount * yGap
                NodeType.CON -> startY + conCount * yGap
                NodeType.EVIDENCE -> startY + evidenceCount * yGap + 45f
                NodeType.TOPIC -> 0f
            }

            val node = ArgumentNode(
                topicId = topicId, type = type, title = title,
                content = content, xPosition = x, yPosition = y
            )
            localIdToNodeId[localId] = node.id
            nodes.add(node)
        }

        val parsedArgumentEdges = parsedEdges.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val fromLocalId = map["from"]?.toString() ?: return@mapNotNull null
            val toLocalId = map["to"]?.toString() ?: return@mapNotNull null
            val fromNodeId = localIdToNodeId[fromLocalId] ?: return@mapNotNull null
            val toNodeId = localIdToNodeId[toLocalId] ?: return@mapNotNull null
            if (fromNodeId == toNodeId) return@mapNotNull null
            val relation = when (map["relation"]?.toString()?.uppercase()) {
                "REFUTES" -> EdgeRelation.REFUTES
                "RELATES" -> EdgeRelation.RELATES
                else -> EdgeRelation.SUPPORTS
            }
            ArgumentEdge(
                topicId = topicId,
                fromNodeId = fromNodeId,
                toNodeId = toNodeId,
                relation = relation
            )
        }.distinctBy { "${it.fromNodeId}:${it.toNodeId}:${it.relation}" }
        val edges = parsedArgumentEdges.ifEmpty { inferDefaultEdges(topicId, nodes) }

        return ArgumentGraph(nodes = nodes, edges = edges)
    }

    private suspend fun repairArgumentJson(
        invalidJson: String,
        aiAdapter: AiProviderAdapter,
        chatConfig: ChatConfig,
        providerConfig: ProviderConfig
    ): String {
        val repairPrompt = buildString {
            append("Repair the following invalid JSON into valid JSON only.\n")
            append("The output must have exactly this shape:\n")
            append("{\"nodes\":[{\"localId\":\"pro1\",\"title\":\"...\",\"type\":\"PRO\",\"content\":\"...\"}],")
            append("\"edges\":[{\"from\":\"pro1\",\"to\":\"con1\",\"relation\":\"REFUTES\"}]}\n")
            append("Allowed localId values: pro1, pro2, pro3, pro4, con1, con2, con3, con4, ev1, ev2, ev3, ev4.\n")
            append("Allowed node types: PRO, CON, EVIDENCE.\n")
            append("Allowed edge relations: SUPPORTS, REFUTES, RELATES.\n")
            append("Return no markdown and no commentary.\n\n")
            append(invalidJson)
        }
        return aiAdapter.chat(
            systemPrompt = "You repair malformed JSON. Return valid JSON only.",
            conversationHistory = listOf(ChatMessage("user", repairPrompt)),
            config = chatConfig,
            providerConfig = providerConfig
        ).content
    }

    private suspend fun repairDebateJson(
        invalidJson: String,
        aiAdapter: AiProviderAdapter,
        chatConfig: ChatConfig,
        providerConfig: ProviderConfig
    ): String {
        val repairPrompt = buildString {
            append("Repair the following invalid JSON into valid JSON only.\n")
            append("""The output must be: {"turns":[{"round":1,"side":"PRO","content":"..."},{"round":1,"side":"CON","content":"..."}]}""")
            append("\nInclude exactly 6 turns: PRO and CON for rounds 1, 2, and 3.\n")
            append("Return no markdown and no commentary.\n\n")
            append(invalidJson)
        }
        return aiAdapter.chat(
            systemPrompt = "You repair malformed JSON. Return valid JSON only.",
            conversationHistory = listOf(ChatMessage("user", repairPrompt)),
            config = chatConfig,
            providerConfig = providerConfig
        ).content
    }

    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start < 0 || end <= start) {
            throw IllegalArgumentException("No JSON object found")
        }
        return trimmed.substring(start, end + 1)
    }

    private fun inferDefaultEdges(topicId: String, nodes: List<ArgumentNode>): List<ArgumentEdge> {
        val pros = nodes.filter { it.type == NodeType.PRO }
        val cons = nodes.filter { it.type == NodeType.CON }
        val evidence = nodes.filter { it.type == NodeType.EVIDENCE }
        val edges = mutableListOf<ArgumentEdge>()

        pros.zip(cons).forEach { (pro, con) ->
            edges.add(ArgumentEdge(topicId = topicId, fromNodeId = pro.id, toNodeId = con.id, relation = EdgeRelation.REFUTES))
            edges.add(ArgumentEdge(topicId = topicId, fromNodeId = con.id, toNodeId = pro.id, relation = EdgeRelation.REFUTES))
        }
        evidence.forEachIndexed { index, evidenceNode ->
            val target = (pros + cons).getOrNull(index) ?: pros.firstOrNull() ?: cons.firstOrNull()
            if (target != null) {
                edges.add(ArgumentEdge(topicId = topicId, fromNodeId = evidenceNode.id, toNodeId = target.id, relation = EdgeRelation.SUPPORTS))
            }
        }
        if (pros.size > 1) {
            edges.add(ArgumentEdge(topicId = topicId, fromNodeId = pros[0].id, toNodeId = pros[1].id, relation = EdgeRelation.RELATES))
        }
        if (cons.size > 1) {
            edges.add(ArgumentEdge(topicId = topicId, fromNodeId = cons[0].id, toNodeId = cons[1].id, relation = EdgeRelation.RELATES))
        }
        return edges.distinctBy { "${it.fromNodeId}:${it.toNodeId}:${it.relation}" }
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
