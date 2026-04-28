package com.aidebate.data.repository

import com.aidebate.data.local.dao.RebuttalAttemptDao
import com.aidebate.data.local.dao.RebuttalSessionDao
import com.aidebate.data.local.entity.RebuttalAttemptEntity
import com.aidebate.data.local.entity.RebuttalSessionEntity
import com.aidebate.data.remote.adapter.AiProviderAdapter
import com.aidebate.data.remote.adapter.ProviderAdapterFactory
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.RebuttalTrainerRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RebuttalTrainerRepositoryImpl @Inject constructor(
    private val sessionDao: RebuttalSessionDao,
    private val attemptDao: RebuttalAttemptDao,
    private val providerConfigRepository: ProviderConfigRepository,
    private val adapterFactory: ProviderAdapterFactory
) : RebuttalTrainerRepository {

    override fun getAllSessions(): Flow<List<RebuttalSession>> =
        sessionDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun createSession(session: RebuttalSession) {
        sessionDao.insert(session.toEntity())
    }

    override suspend fun getSession(sessionId: String): RebuttalSession? =
        sessionDao.getById(sessionId)?.toDomain()

    override fun getAttempts(sessionId: String): Flow<List<RebuttalAttempt>> =
        attemptDao.getBySessionId(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveAttempt(attempt: RebuttalAttempt) {
        attemptDao.insert(attempt.toEntity())
    }

    private suspend fun getAdapter(): Pair<AiProviderAdapter, ProviderConfig> {
        val configs = providerConfigRepository.getEnabledConfigs().first()
        val config = configs.firstOrNull() ?: throw Exception("No AI provider configured")
        val adapter = adapterFactory.getAdapter(config.provider)
        return adapter to config
    }

    suspend fun generateRebuttalPrompt(topicTitle: String, userSide: String, difficulty: String): String {
        val (adapter, config) = getAdapter()
        val difficultyDesc = when (difficulty) {
            "easy" -> "with an obvious flaw"
            "hard" -> "that is well-structured and subtle"
            else -> "with a subtle flaw"
        }
        val prompt = "Generate a debate argument for the topic \"$topicTitle\" " +
            "that argues ${if (userSide == "AGAINST") "AGAINST" else "FOR"} the topic, " +
            "$difficultyDesc. The argument should be 2-4 sentences, persuasive, and ready for rebuttal practice."
        val result = adapter.chat(
            systemPrompt = "You simulate a skilled debater generating practice arguments.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = ChatConfig(model = config.modelName),
            providerConfig = config
        )
        return result.content
    }

    suspend fun scoreRebuttal(
        sessionId: String,
        promptArgument: String,
        userResponse: String
    ): RebuttalAttempt {
        val (adapter, config) = getAdapter()
        val prompt = buildString {
            append("Score this rebuttal on 4 criteria (each out of 25):\n")
            append("1. Logic: Did they identify logical gaps?\n")
            append("2. Clarity: Is the rebuttal clear and well-structured?\n")
            append("3. Persuasion: How convincing is it?\n")
            append("4. Evidence: Did they use sound reasoning?\n\n")
            append("Original argument: \"$promptArgument\"\n\n")
            append("Rebuttal: \"$userResponse\"\n\n")
            append("Return ONLY a JSON object with fields: logicScore (int), clarityScore (int), ")
            append("persuasionScore (int), evidenceScore (int), totalScore (int), feedback (string, 2-3 sentences of specific advice).")
        }
        val result = adapter.chat(
            systemPrompt = "You are a debate coach scoring rebuttals. Always return valid JSON.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = ChatConfig(model = config.modelName, temperature = 0.3),
            providerConfig = config
        )
        return parseScoreResult(sessionId, result.content)
    }

    suspend fun generateExplanation(
        topicTitle: String,
        promptArgument: String,
        userResponse: String,
        attempt: RebuttalAttempt
    ): RebuttalExplanation {
        val (adapter, config) = getAdapter()
        val prompt = buildString {
            append("You are a debate coach. Provide a detailed breakdown of this rebuttal attempt.\n\n")
            append("Topic: \"$topicTitle\"\n")
            append("Original argument: \"$promptArgument\"\n")
            append("User's rebuttal: \"$userResponse\"\n")
            append("Scores: Logic=${attempt.logicScore}/25, Clarity=${attempt.clarityScore}/25, ")
            append("Persuasion=${attempt.persuasionScore}/25, Evidence=${attempt.evidenceScore}/25\n\n")
            append("Return ONLY valid JSON with this structure:\n")
            append("""{"breakdown": [{"category": "Logic", "score": ${attempt.logicScore}, "strength": "what they did well", "weakness": "what could improve", "suggestion": "specific advice"}, ...], "overallAdvice": "2-3 sentence summary", "keyTakeaway": "one key takeaway"}""")
        }
        val result = adapter.chat(
            systemPrompt = "You are a detailed debate coach. Always return valid JSON.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = ChatConfig(model = config.modelName, temperature = 0.4),
            providerConfig = config
        )
        return parseExplanationResult(result.content)
    }

    suspend fun chatAboutRebuttal(
        topicTitle: String,
        promptArgument: String,
        userResponse: String,
        attempt: RebuttalAttempt,
        messages: List<RebuttalChatMessage>
    ): RebuttalChatMessage {
        val (adapter, config) = getAdapter()
        val history = buildList {
            add(ChatMessage("system", "You are a helpful debate coach. The user just completed a rebuttal practice on \"$topicTitle\". Original argument: \"$promptArgument\". Their rebuttal: \"$userResponse\". Scores: Logic=${attempt.logicScore}/25, Clarity=${attempt.clarityScore}/25, Persuasion=${attempt.persuasionScore}/25, Evidence=${attempt.evidenceScore}/25. Answer their questions about their performance and help them improve."))
            messages.forEach { msg ->
                add(ChatMessage(if (msg.role == "ai") "assistant" else "user", msg.content))
            }
        }
        val result = adapter.chat(
            systemPrompt = "You are a helpful debate coach helping a user improve their rebuttal skills.",
            conversationHistory = history.drop(1),
            config = ChatConfig(model = config.modelName, temperature = 0.4),
            providerConfig = config
        )
        return RebuttalChatMessage("ai", result.content)
    }

    suspend fun analyzeFallacies(text: String): List<FallacyResult> {
        val (adapter, config) = getAdapter()
        val prompt = buildString {
            append("Analyze the following text for logical fallacies.\n")
            append("Common fallacies include: ad hominem, straw man, false dichotomy, ")
            append("slippery slope, circular reasoning, hasty generalization, appeal to authority, ")
            append("red herring, bandwagon, false cause, appeal to emotion, no true scotsman, ")
            append("tu quoque, begging the question, equivocation.\n\n")
            append("Text: \"$text\"\n\n")
            append("Return ONLY a JSON array of objects with fields: ")
            append("name (fallacy name), quotedText (the exact quote from the text), ")
            append("explanation (1-2 sentences explaining why this is that fallacy). ")
            append("If no fallacies found, return an empty array [].")
        }
        val result = adapter.chat(
            systemPrompt = "You are a logic expert identifying fallacies. Always return valid JSON.",
            conversationHistory = listOf(ChatMessage("user", prompt)),
            config = ChatConfig(model = config.modelName, temperature = 0.2),
            providerConfig = config
        )
        return parseFallacyResult(result.content)
    }

    private fun parseScoreResult(sessionId: String, json: String): RebuttalAttempt {
        val cleanedJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = moshi.adapter<Map<String, Any>>(mapType).fromJson(cleanedJson) ?: emptyMap()
        return RebuttalAttempt(
            sessionId = sessionId,
            promptArgument = "",
            userResponse = "",
            logicScore = (map["logicScore"] as? Number)?.toInt() ?: 0,
            clarityScore = (map["clarityScore"] as? Number)?.toInt() ?: 0,
            persuasionScore = (map["persuasionScore"] as? Number)?.toInt() ?: 0,
            evidenceScore = (map["evidenceScore"] as? Number)?.toInt() ?: 0,
            totalScore = (map["totalScore"] as? Number)?.toInt() ?: 0,
            feedback = map["feedback"]?.toString() ?: ""
        )
    }

    private fun parseExplanationResult(json: String): RebuttalExplanation {
        val cleanedJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        // Only escape control characters inside string values, not structural whitespace
        val sanitized = sanitizeJsonStrings(cleanedJson)
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val map = try {
            moshi.adapter<Map<String, Any>>(mapType).fromJson(sanitized)
        } catch (_: Exception) {
            emptyMap()
        } ?: emptyMap()

        val rawBreakdown = (map["breakdown"] as? List<Map<String, Any>>) ?: emptyList()
        val breakdown = rawBreakdown.map { b ->
            ScoreBreakdown(
                category = b["category"]?.toString() ?: "",
                score = (b["score"] as? Number)?.toInt() ?: 0,
                strength = b["strength"]?.toString() ?: "",
                weakness = b["weakness"]?.toString() ?: "",
                suggestion = b["suggestion"]?.toString() ?: ""
            )
        }
        return RebuttalExplanation(
            breakdown = breakdown,
            overallAdvice = map["overallAdvice"]?.toString() ?: "",
            keyTakeaway = map["keyTakeaway"]?.toString() ?: ""
        )
    }

    private fun parseFallacyResult(json: String): List<FallacyResult> {
        val cleanedJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val sanitized = sanitizeJsonStrings(cleanedJson)
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, Map::class.java)
        val parsed = try {
            moshi.adapter<List<Map<String, Any>>>(listType).fromJson(sanitized)
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return parsed.map { map ->
            FallacyResult(
                name = map["name"]?.toString() ?: "Unknown",
                quotedText = map["quotedText"]?.toString() ?: "",
                explanation = map["explanation"]?.toString() ?: ""
            )
        }
    }
}

/**
 * Escapes control characters only inside JSON string values.
 * Unlike sanitizeJson (which broke structural whitespace), this tracks
 * whether we're inside a quote-delimited string and only escapes there.
 */
private fun sanitizeJsonStrings(json: String): String {
    val result = StringBuilder(json.length)
    var inString = false
    var i = 0
    while (i < json.length) {
        val c = json[i]
        when {
            // Toggle inString on unescaped quotes
            c == '"' && (i == 0 || json[i - 1] != '\\') -> {
                inString = !inString
                result.append(c)
            }
            inString && c == '\n' -> result.append("\\n")
            inString && c == '\r' -> result.append("\\r")
            inString && c == '\t' -> result.append("\\t")
            else -> result.append(c)
        }
        i++
    }
    return result.toString()
}

private fun RebuttalSession.toEntity() = RebuttalSessionEntity(
    id = id, topicId = topicId, topicTitle = topicTitle,
    userSide = userSide, difficulty = difficulty, createdAt = createdAt
)

private fun RebuttalSessionEntity.toDomain() = RebuttalSession(
    id = id, topicId = topicId, topicTitle = topicTitle,
    userSide = userSide, difficulty = difficulty, createdAt = createdAt
)

private fun RebuttalAttempt.toEntity() = RebuttalAttemptEntity(
    id = id, sessionId = sessionId, promptArgument = promptArgument,
    userResponse = userResponse, timeLimitSec = timeLimitSec,
    timeTakenMs = timeTakenMs, logicScore = logicScore, clarityScore = clarityScore,
    persuasionScore = persuasionScore, evidenceScore = evidenceScore,
    totalScore = totalScore, feedback = feedback, createdAt = createdAt
)

private fun RebuttalAttemptEntity.toDomain() = RebuttalAttempt(
    id = id, sessionId = sessionId, promptArgument = promptArgument,
    userResponse = userResponse, timeLimitSec = timeLimitSec,
    timeTakenMs = timeTakenMs, logicScore = logicScore, clarityScore = clarityScore,
    persuasionScore = persuasionScore, evidenceScore = evidenceScore,
    totalScore = totalScore, feedback = feedback, createdAt = createdAt
)
