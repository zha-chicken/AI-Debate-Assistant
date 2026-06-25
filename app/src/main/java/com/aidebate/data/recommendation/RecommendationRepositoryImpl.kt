package com.aidebate.data.recommendation

import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.DebateResult
import com.aidebate.domain.model.DebateSessionSummary
import com.aidebate.domain.model.DebateTopic
import com.aidebate.domain.model.DebateTrainingTag
import com.aidebate.domain.model.MBTIType
import com.aidebate.domain.model.RecommendationFeedback
import com.aidebate.domain.model.RecommendationFeedbackReasonType
import com.aidebate.domain.model.RecommendationFeedbackSentiment
import com.aidebate.domain.model.RecommendationReasonType
import com.aidebate.domain.model.SessionStatus
import com.aidebate.domain.model.TopicRecommendation
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.RecommendationRepository
import com.aidebate.domain.repository.SettingsRepository
import com.aidebate.domain.repository.TopicRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val KEY_MBTI_TYPE = "recommendation_mbti_type"
private const val KEY_RECOMMENDATION_FEEDBACK = "recommendation_feedback_v1"

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository,
    private val settingsRepository: SettingsRepository,
) : RecommendationRepository {

    override fun getRecommendations(limit: Int): Flow<List<TopicRecommendation>> =
        combine(
            topicRepository.getAllTopics(),
            debateRepository.getAllSessions(),
            debateRepository.getAllResults(),
            observeMbti(),
            observeFeedbackList()
        ) { topics, sessions, results, mbti, feedback ->
            buildRecommendations(
                topics = topics,
                sessions = sessions,
                results = results,
                mbti = mbti,
                feedback = feedback,
                limit = limit
            )
        }

    override fun observeMbti(): Flow<MBTIType?> =
        settingsRepository.observeString(KEY_MBTI_TYPE).map { raw ->
            raw?.let { runCatching { MBTIType.valueOf(it) }.getOrNull() }
        }

    override suspend fun setMbti(type: MBTIType?) {
        settingsRepository.setString(KEY_MBTI_TYPE, type?.name.orEmpty())
    }

    override fun observeFeedback(sessionId: String): Flow<RecommendationFeedback?> =
        observeFeedbackList().map { feedback ->
            feedback.firstOrNull { it.sessionId == sessionId }
        }

    override suspend fun recordFeedback(
        sessionId: String,
        topicTitle: String,
        category: String,
        sentiment: RecommendationFeedbackSentiment,
        reasonType: RecommendationFeedbackReasonType
    ) {
        val current = parseFeedbackList(settingsRepository.getString(KEY_RECOMMENDATION_FEEDBACK))
            .filterNot { it.sessionId == sessionId }
            .toMutableList()
        current += RecommendationFeedback(
            sessionId = sessionId,
            topicTitle = topicTitle,
            category = category,
            sentiment = sentiment,
            reasonType = reasonType
        )
        settingsRepository.setString(KEY_RECOMMENDATION_FEEDBACK, encodeFeedbackList(current.takeLast(200)))
    }

    private fun observeFeedbackList(): Flow<List<RecommendationFeedback>> =
        settingsRepository.observeString(KEY_RECOMMENDATION_FEEDBACK).map(::parseFeedbackList)

    private fun buildRecommendations(
        topics: List<DebateTopic>,
        sessions: List<DebateSessionSummary>,
        results: List<DebateResult>,
        mbti: MBTIType?,
        feedback: List<RecommendationFeedback>,
        limit: Int
    ): List<TopicRecommendation> {
        val topicsByTitle = topics.associateBy { normalizeTitle(it.title) }
        val eligibleSessions = sessions.filter { it.mode != DebateMode.AI_VS_AI }
        val engaged = eligibleSessions.filter {
            it.turnCount > 0 || it.status == SessionStatus.COMPLETED
        }
        if (engaged.size < 2) return emptyList()

        val favoriteCategory = mostCommon(
            engaged.mapNotNull { session -> topicsByTitle[normalizeTitle(session.topicTitle)]?.category }
        ) ?: return emptyList()

        val eligibleSessionIds = eligibleSessions.map { it.id }.toSet()
        val resultTexts = results
            .filter { it.sessionId in eligibleSessionIds }
            .map { it.summary }
            .filter { it.isNotBlank() }
        val weakness = buildWeaknessSignals(resultTexts).firstOrNull()
        val weaknessKeywords = recommendationKeywordsForWeakness(weakness?.title)
        val mbtiKeywords = recommendationKeywordsForMbti(mbti)
        val feedbackScores = categoryFeedbackScores(feedback, sessions)

        val eligibleDebatedCounts = eligibleSessions
            .groupingBy { normalizeTitle(it.topicTitle) }
            .eachCount()
        val recentTitles = sessions
            .sortedByDescending { it.createdAt }
            .take(5)
            .map { normalizeTitle(it.topicTitle) }
            .toSet()
        val favoriteCategoryKey = normalizeTitle(favoriteCategory)

        val ranked = topics.map { topic ->
            val topicKey = normalizeTitle(topic.title)
            val categoryKey = normalizeTitle(topic.category)
            val tags = inferTrainingTags(topic)
            val text = topicRecommendationText(topic, tags)
            val tagMatches = recommendationTagMatches(tags, weakness?.title)
            val weaknessMatches = keywordMatches(text, weaknessKeywords)
            val mbtiMatches = keywordMatches(text, mbtiKeywords)
            val feedbackScore = feedbackScores[categoryKey] ?: 0
            val score =
                (if (categoryKey == favoriteCategoryKey) 16 else 0) +
                    tagMatches * 52 +
                    weaknessMatches * 18 +
                    mbtiMatches * 5 +
                    feedbackScore -
                    (eligibleDebatedCounts[topicKey] ?: 0) * 9 -
                    (if (topicKey in recentTitles) 18 else 0)

            RankedTopic(
                topic = topic,
                score = score,
                tagMatches = tagMatches,
                weaknessMatches = weaknessMatches,
                mbtiMatches = mbtiMatches,
                feedbackScore = feedbackScore
            )
        }.sortedWith(
            compareByDescending<RankedTopic> { it.score }
                .thenBy { eligibleDebatedCounts[normalizeTitle(it.topic.title)] ?: 0 }
                .thenBy { it.topic.title }
        )

        val selected = mutableListOf<RankedTopic>()
        val categoryCounts = mutableMapOf<String, Int>()
        val targetCount = limit.coerceAtLeast(1)
        while (selected.size < targetCount) {
            val next = ranked
                .filterNot { candidate -> selected.any { it.topic.id == candidate.topic.id } }
                .maxWithOrNull { left, right ->
                    val leftAdjusted = left.score - (categoryCounts[normalizeTitle(left.topic.category)] ?: 0) * 24
                    val rightAdjusted = right.score - (categoryCounts[normalizeTitle(right.topic.category)] ?: 0) * 24
                    when {
                        leftAdjusted != rightAdjusted -> leftAdjusted.compareTo(rightAdjusted)
                        left.topic.title != right.topic.title -> right.topic.title.compareTo(left.topic.title)
                        else -> 0
                    }
                } ?: break
            selected += next
            categoryCounts[normalizeTitle(next.topic.category)] =
                (categoryCounts[normalizeTitle(next.topic.category)] ?: 0) + 1
        }

        return selected.map { item ->
            val reasonType = when {
                weakness != null && (item.tagMatches > 0 || item.weaknessMatches > 0) ->
                    RecommendationReasonType.WEAKNESS
                item.feedbackScore != 0 ->
                    RecommendationReasonType.FEEDBACK
                mbti != null && item.mbtiMatches > 0 ->
                    RecommendationReasonType.MBTI
                else ->
                    RecommendationReasonType.CATEGORY
            }
            val focus = when (reasonType) {
                RecommendationReasonType.WEAKNESS -> weakness?.title ?: favoriteCategory
                RecommendationReasonType.FEEDBACK ->
                    if (item.feedbackScore > 0) "Liked category" else "Disliked category"
                RecommendationReasonType.MBTI -> mbti?.name ?: favoriteCategory
                RecommendationReasonType.CATEGORY -> favoriteCategory
            }
            TopicRecommendation(
                topic = item.topic,
                reason = reasonType.name,
                focus = focus,
                matchedSignal = weakness?.title,
                score = item.score,
                reasonType = reasonType,
                favoriteCategory = favoriteCategory
            )
        }
    }

    private fun buildWeaknessSignals(texts: List<String>): List<WeaknessSignal> {
        val definitions = listOf(
            WeaknessSignalDefinition("Needs stronger evidence", listOf("evidence", "unsupported", "data", "example", "proof", "证据", "数据", "例子", "支撑", "缺少")),
            WeaknessSignalDefinition("Needs more direct clash", listOf("clash", "respond", "rebut", "answer", "engage", "回应", "反驳", "交锋", "正面回答")),
            WeaknessSignalDefinition("Needs clearer structure", listOf("structure", "framework", "organize", "clarity", "clear", "结构", "框架", "组织", "清晰")),
            WeaknessSignalDefinition("Needs stronger impact weighing", listOf("weigh", "impact", "compare", "priority", "outweigh", "比较", "影响", "权衡", "优先")),
            WeaknessSignalDefinition("Needs clearer definitions", listOf("definition", "define", "scope", "framing", "motion", "unclear term", "定义", "范围", "框定", "概念", "不清"))
        )
        return definitions
            .mapNotNull { definition ->
                val count = texts.sumOf { text -> keywordMatches(text, definition.keywords) }
                if (count > 0) WeaknessSignal(definition.title, count) else null
            }
            .sortedByDescending { it.score }
    }

    private fun categoryFeedbackScores(
        feedback: List<RecommendationFeedback>,
        sessions: List<DebateSessionSummary>
    ): Map<String, Int> {
        val sessionsById = sessions.associateBy { it.id }
        val scores = mutableMapOf<String, Int>()
        feedback
            .filter { it.reasonType == RecommendationFeedbackReasonType.CATEGORY }
            .forEach { item ->
                val session = sessionsById[item.sessionId]
                if (session?.mode == DebateMode.AI_VS_AI) return@forEach
                val key = normalizeTitle(item.category)
                scores[key] = (scores[key] ?: 0) + when (item.sentiment) {
                    RecommendationFeedbackSentiment.LIKE -> 12
                    RecommendationFeedbackSentiment.DISLIKE -> -18
                }
            }
        return scores
    }

    private data class RankedTopic(
        val topic: DebateTopic,
        val score: Int,
        val tagMatches: Int,
        val weaknessMatches: Int,
        val mbtiMatches: Int,
        val feedbackScore: Int
    )

    private data class WeaknessSignal(val title: String, val score: Int)
    private data class WeaknessSignalDefinition(val title: String, val keywords: List<String>)
}

internal fun inferTrainingTags(topic: DebateTopic): List<DebateTrainingTag> {
    val text = "${topic.title} ${topic.category} ${topic.description}".lowercase()
    val tags = mutableListOf<DebateTrainingTag>()
    fun add(tag: DebateTrainingTag) {
        if (tag !in tags) tags += tag
    }

    if (text.containsAny("data", "evidence", "statistics", "study", "research", "tests", "diagnoses", "predictive", "misinformation", "climate", "health", "crime", "tax", "funding", "cost", "energy", "security", "safety", "证据", "数据", "研究")) add(DebateTrainingTag.EVIDENCE_HEAVY)
    if (text.containsAny("define", "definition", "scope", "legal", "rights", "free will", "morality", "objective", "privacy", "copyright", "authorship", "consent", "public interest", "tradition", "justice", "mercy", "patriotism", "定义", "范围", "权利")) add(DebateTrainingTag.DEFINITION_HEAVY)
    if (text.containsAny("policy", "regulation", "regulated", "ban", "banned", "required", "mandatory", "government", "governments", "law", "tax", "fund", "public services", "schools", "admissions", "voting", "enforced", "监管", "政策", "禁止", "强制")) add(DebateTrainingTag.POLICY_MECHANISM)
    if (text.containsAny("cost", "budget", "funding", "enforcement", "practical", "feasibility", "implementation", "liability", "access", "market", "taxes", "measurement", "teacher readiness", "transport", "可行", "执行", "成本")) add(DebateTrainingTag.FEASIBILITY)
    if (text.containsAny("harm", "benefit", "impact", "inequality", "wellbeing", "mental health", "suffering", "climate", "jobs", "safety", "future generations", "public health", "quality", "影响", "伤害", "就业", "健康")) add(DebateTrainingTag.IMPACT_WEIGHING)
    if (text.containsAny("compare", "weigh", "tradeoff", "versus", "over", "priority", "balance", "autonomy", "collective", "freedom", "safety", "equality over excellence", "truth matter more", "比较", "权衡", "优先")) add(DebateTrainingTag.COMPARATIVE_WEIGHING)
    if (text.containsAny("autonomy", "freedom", "rights", "privacy", "consent", "dignity", "choice", "civil liberties", "expression", "parental rights", "right to", "自由", "自主", "隐私", "尊严")) add(DebateTrainingTag.RIGHTS_AUTONOMY)
    if (text.containsAny("fairness", "justice", "values", "moral", "ethics", "tradition", "identity", "community", "culture", "religion", "harm", "equality", "merit", "公平", "正义", "道德", "伦理")) add(DebateTrainingTag.VALUE_CLASH)
    if (text.containsAny("students", "children", "parents", "teachers", "companies", "governments", "citizens", "immigrants", "workers", "cities", "patients", "artists", "athletes", "animals", "future generations", "用户", "学生", "政府", "企业")) add(DebateTrainingTag.STAKEHOLDER_ANALYSIS)
    if (text.containsAny("cause", "causal", "because", "lead to", "replace", "automation", "addiction", "deterrence", "predictive", "outcomes", "effects", "incentives", "导致", "因果", "激励")) add(DebateTrainingTag.CAUSAL_REASONING)
    if (text.containsAny("speech", "hate speech", "offensive", "censorship", "public discourse", "civil disobedience", "controversial", "political", "misinformation", "debate", "argument")) add(DebateTrainingTag.DIRECT_CLASH)
    if (text.containsAny("framework", "standards", "assessment", "accountability", "burden", "discipline", "curriculum", "governance", "judges", "admissions", "grades", "结构", "标准", "框架")) add(DebateTrainingTag.STRUCTURE_BURDEN)

    if (tags.isEmpty()) {
        add(DebateTrainingTag.VALUE_CLASH)
        add(DebateTrainingTag.STAKEHOLDER_ANALYSIS)
    }
    return tags.take(5)
}

private fun recommendationTagMatches(tags: List<DebateTrainingTag>, weaknessTitle: String?): Int {
    val targets = recommendationTrainingTagsForWeakness(weaknessTitle)
    if (targets.isEmpty()) return 0
    return targets.count { it in tags }
}

private fun recommendationTrainingTagsForWeakness(weaknessTitle: String?): List<DebateTrainingTag> {
    val normalized = weaknessTitle?.lowercase() ?: return emptyList()
    return when {
        normalized.contains("evidence") || normalized.contains("信息不实") || normalized.contains("证据") ->
            listOf(DebateTrainingTag.EVIDENCE_HEAVY, DebateTrainingTag.CAUSAL_REASONING, DebateTrainingTag.POLICY_MECHANISM, DebateTrainingTag.FEASIBILITY)
        normalized.contains("definition") || normalized.contains("scope") || normalized.contains("定义") || normalized.contains("范围") ->
            listOf(DebateTrainingTag.DEFINITION_HEAVY, DebateTrainingTag.STRUCTURE_BURDEN, DebateTrainingTag.RIGHTS_AUTONOMY)
        normalized.contains("clash") || normalized.contains("causal") || normalized.contains("direct") || normalized.contains("交锋") || normalized.contains("反驳") ->
            listOf(DebateTrainingTag.DIRECT_CLASH, DebateTrainingTag.VALUE_CLASH, DebateTrainingTag.COMPARATIVE_WEIGHING, DebateTrainingTag.CAUSAL_REASONING)
        normalized.contains("impact") || normalized.contains("weigh") || normalized.contains("影响") || normalized.contains("权衡") ->
            listOf(DebateTrainingTag.IMPACT_WEIGHING, DebateTrainingTag.COMPARATIVE_WEIGHING, DebateTrainingTag.STAKEHOLDER_ANALYSIS)
        normalized.contains("structure") || normalized.contains("slow") || normalized.contains("结构") || normalized.contains("节奏") ->
            listOf(DebateTrainingTag.STRUCTURE_BURDEN, DebateTrainingTag.POLICY_MECHANISM, DebateTrainingTag.COMPARATIVE_WEIGHING)
        else -> emptyList()
    }
}

private fun recommendationKeywordsForWeakness(weaknessTitle: String?): List<String> {
    val value = weaknessTitle ?: return emptyList()
    return when {
        value.contains("evidence", true) ->
            listOf("study", "data", "evidence", "statistics", "regulation", "screen", "tax", "surveillance", "misinformation", "AI", "climate", "energy", "health", "tests", "证据", "数据", "监管", "税", "气候", "健康")
        value.contains("definition", true) ->
            listOf("legal", "rights", "free will", "capitalism", "anonymity", "euthanasia", "animals", "privacy", "autonomy", "definition", "scope", "合法", "权利", "自由意志", "资本主义", "匿名", "安乐死", "隐私", "定义")
        value.contains("clash", true) || value.contains("causal", true) ->
            listOf("versus", "harm", "benefit", "regulation", "ban", "replace", "tax", "mandatory", "priority", "clash", "利弊", "监管", "禁止", "取代", "强制", "优先")
        value.contains("impact", true) ->
            listOf("cost", "economics", "environment", "climate", "jobs", "health", "cities", "public", "future", "impact", "经济", "环境", "气候", "就业", "健康", "城市", "影响")
        value.contains("structure", true) || value.contains("slow", true) ->
            listOf("school", "homework", "work", "admissions", "public transit", "structured", "education", "工作", "教育", "学校", "作业", "结构")
        else -> emptyList()
    }
}

private fun recommendationKeywordsForMbti(mbti: MBTIType?): List<String> = when (mbti) {
    MBTIType.ISTJ -> listOf("law", "regulation", "accountability", "mandatory", "schools", "public", "work", "tests", "法律", "监管", "责任", "强制", "学校", "公共", "工作")
    MBTIType.ISFJ -> listOf("education", "health", "safety", "public", "schools", "society", "responsibility", "community", "教育", "健康", "安全", "公共", "学校", "社会", "责任")
    MBTIType.INFJ -> listOf("ethics", "rights", "fairness", "society", "dignity", "education", "autonomy", "future", "伦理", "权利", "公平", "社会", "尊严", "教育", "自主")
    MBTIType.INTJ -> listOf("systems", "policy", "science", "technology", "artificial intelligence", "economics", "future", "free will", "系统", "政策", "科学", "科技", "经济", "未来", "自由意志")
    MBTIType.ISTP -> listOf("technology", "evidence", "data", "energy", "cities", "transit", "privacy", "safety", "科技", "证据", "数据", "能源", "城市", "交通", "隐私", "安全")
    MBTIType.ISFP -> listOf("animals", "rights", "privacy", "autonomy", "environment", "dignity", "health", "choice", "动物", "权利", "隐私", "自主", "环境", "尊严", "健康", "选择")
    MBTIType.INFP -> listOf("ethics", "justice", "rights", "animals", "education", "society", "freedom", "dignity", "伦理", "正义", "权利", "动物", "教育", "社会", "自由", "尊严")
    MBTIType.INTP -> listOf("logic", "science", "philosophy", "free will", "artificial intelligence", "evidence", "privacy", "capitalism", "逻辑", "科学", "哲学", "自由意志", "证据", "隐私", "资本主义")
    MBTIType.ESTP -> listOf("cities", "sports", "public", "health", "media", "work", "transit", "practical", "城市", "体育", "公共", "健康", "媒体", "工作", "交通", "实践")
    MBTIType.ESFP -> listOf("society", "social media", "education", "health", "cities", "public", "connection", "entertainment", "社会", "社交媒体", "教育", "健康", "城市", "公共", "连接")
    MBTIType.ENFP -> listOf("possibility", "creativity", "education", "society", "media", "rights", "future", "artificial intelligence", "可能性", "创造力", "教育", "社会", "媒体", "权利", "未来")
    MBTIType.ENTP -> listOf("innovation", "technology", "artificial intelligence", "policy", "economics", "regulation", "free speech", "capitalism", "创新", "科技", "政策", "经济", "监管", "言论自由", "资本主义")
    MBTIType.ESTJ -> listOf("law", "policy", "regulation", "work", "schools", "mandatory", "economics", "accountability", "法律", "政策", "监管", "工作", "学校", "强制", "经济", "责任")
    MBTIType.ESFJ -> listOf("community", "education", "health", "society", "public", "schools", "welfare", "responsibility", "社区", "教育", "健康", "社会", "公共", "学校", "福利", "责任")
    MBTIType.ENFJ -> listOf("education", "rights", "society", "leadership", "fairness", "community", "mental health", "dignity", "教育", "权利", "社会", "领导力", "公平", "社区", "心理健康", "尊严")
    MBTIType.ENTJ -> listOf("leadership", "policy", "economics", "systems", "regulation", "work", "capitalism", "artificial intelligence", "领导力", "政策", "经济", "系统", "监管", "工作", "资本主义")
    null -> emptyList()
}

private fun topicRecommendationText(topic: DebateTopic, tags: List<DebateTrainingTag>): String =
    "${topic.title} ${topic.category} ${topic.description} ${tags.joinToString(" ") { it.name }}"

private fun keywordMatches(text: String, keywords: List<String>): Int {
    val lower = text.lowercase()
    return keywords.count { lower.contains(it.lowercase()) }
}

private fun String.containsAny(vararg keywords: String): Boolean =
    keywords.any { contains(it, ignoreCase = true) }

private fun normalizeTitle(title: String): String =
    title.trim().trimEnd('?').lowercase()

private fun <T> mostCommon(values: List<T>): T? =
    values.groupingBy { it }.eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<T, Int>> { it.value }.thenBy { it.key.toString() })
        .firstOrNull()
        ?.key

private fun parseFeedbackList(raw: String?): List<RecommendationFeedback> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            RecommendationFeedback(
                id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                sessionId = obj.optString("sessionId"),
                topicTitle = obj.optString("topicTitle"),
                category = obj.optString("category"),
                sentiment = runCatching { RecommendationFeedbackSentiment.valueOf(obj.optString("sentiment")) }.getOrDefault(RecommendationFeedbackSentiment.LIKE),
                reasonType = runCatching { RecommendationFeedbackReasonType.valueOf(obj.optString("reasonType")) }.getOrDefault(RecommendationFeedbackReasonType.TECHNIQUE),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }.getOrElse { emptyList() }
}

private fun encodeFeedbackList(feedback: List<RecommendationFeedback>): String {
    val array = JSONArray()
    feedback.forEach { item ->
        array.put(JSONObject().apply {
            put("id", item.id)
            put("sessionId", item.sessionId)
            put("topicTitle", item.topicTitle)
            put("category", item.category)
            put("sentiment", item.sentiment.name)
            put("reasonType", item.reasonType.name)
            put("createdAt", item.createdAt)
        })
    }
    return array.toString()
}
