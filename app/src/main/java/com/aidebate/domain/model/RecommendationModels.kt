package com.aidebate.domain.model

import java.util.UUID

enum class MBTIType {
    INTJ, INTP, ENTJ, ENTP,
    INFJ, INFP, ENFJ, ENFP,
    ISTJ, ISFJ, ESTJ, ESFJ,
    ISTP, ISFP, ESTP, ESFP
}

enum class DebateTrainingTag {
    EVIDENCE_HEAVY,
    DEFINITION_HEAVY,
    IMPACT_WEIGHING,
    POLICY_MECHANISM,
    VALUE_CLASH,
    RIGHTS_AUTONOMY,
    STAKEHOLDER_ANALYSIS,
    CAUSAL_REASONING,
    COMPARATIVE_WEIGHING,
    FEASIBILITY,
    DIRECT_CLASH,
    STRUCTURE_BURDEN
}

enum class RecommendationFeedbackSentiment {
    LIKE,
    DISLIKE
}

enum class RecommendationFeedbackReasonType {
    CATEGORY,
    TECHNIQUE
}

data class RecommendationFeedback(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val topicTitle: String,
    val category: String,
    val sentiment: RecommendationFeedbackSentiment,
    val reasonType: RecommendationFeedbackReasonType,
    val createdAt: Long = System.currentTimeMillis()
)

data class MemorySignal(
    val title: String,
    val detail: String,
    val score: Int,
    val evidenceCount: Int
)

enum class RecommendationReasonType {
    WEAKNESS,
    FEEDBACK,
    MBTI,
    CATEGORY
}

data class TopicRecommendation(
    val topic: DebateTopic,
    val reason: String,
    val focus: String,
    val matchedSignal: String? = null,
    val score: Int = 0,
    val reasonType: RecommendationReasonType = RecommendationReasonType.CATEGORY,
    val favoriteCategory: String = ""
)
