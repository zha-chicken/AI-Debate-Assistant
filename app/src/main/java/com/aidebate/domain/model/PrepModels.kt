package com.aidebate.domain.model

import java.util.UUID

enum class NodeType { TOPIC, PRO, CON, EVIDENCE }

enum class EdgeRelation { SUPPORTS, REFUTES, RELATES }

data class ArgumentNode(
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val type: NodeType,
    val title: String,
    val content: String = "",
    val parentId: String? = null,
    val xPosition: Float = 0f,
    val yPosition: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

data class ArgumentEdge(
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relation: EdgeRelation = EdgeRelation.SUPPORTS
)

data class ArgumentGraph(
    val nodes: List<ArgumentNode>,
    val edges: List<ArgumentEdge>
)

data class RebuttalSession(
    val id: String = UUID.randomUUID().toString(),
    val topicId: String,
    val topicTitle: String = "",
    val userSide: String = "AGAINST",
    val difficulty: String = "medium",
    val createdAt: Long = System.currentTimeMillis()
)

data class RebuttalAttempt(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val promptArgument: String,
    val userResponse: String,
    val timeLimitSec: Int = 60,
    val timeTakenMs: Long = 0,
    val logicScore: Int = 0,
    val clarityScore: Int = 0,
    val persuasionScore: Int = 0,
    val evidenceScore: Int = 0,
    val totalScore: Int = 0,
    val feedback: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class FallacyResult(
    val name: String,
    val quotedText: String,
    val explanation: String
)

data class FallacyReference(
    val name: String,
    val description: String,
    val example: String
)

data class ScoreBreakdown(
    val category: String,      // "Logic", "Clarity", "Persuasion", "Evidence"
    val score: Int,            // out of 25
    val strength: String,      // what the user did well
    val weakness: String,      // what could be improved
    val suggestion: String     // specific advice for next time
)

data class RebuttalExplanation(
    val breakdown: List<ScoreBreakdown>,
    val overallAdvice: String,
    val keyTakeaway: String
)

data class RebuttalChatMessage(
    val role: String,          // "user" or "ai"
    val content: String
)
