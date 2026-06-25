package com.aidebate.domain.repository

import com.aidebate.domain.model.*
import kotlinx.coroutines.flow.Flow

interface DebateRepository {
    suspend fun createSession(session: DebateSession)
    suspend fun updateSession(session: DebateSession)
    fun getSession(sessionId: String): Flow<DebateSession?>
    suspend fun addTurn(turn: DebateTurn)
    suspend fun updateTurn(turn: DebateTurn)
    fun getTurns(sessionId: String): Flow<List<DebateTurn>>
    suspend fun saveResult(result: DebateResult)
    fun getResult(sessionId: String): Flow<DebateResult?>
    fun getAllResults(): Flow<List<DebateResult>>
    fun getAllSessions(): Flow<List<DebateSessionSummary>>
    suspend fun deleteSession(sessionId: String)
}

interface TopicRepository {
    fun getAllTopics(): Flow<List<DebateTopic>>
    suspend fun saveTopic(topic: DebateTopic)
    suspend fun getTopic(topicId: String): DebateTopic?
    suspend fun seedPredefinedTopics()
}

interface ProviderConfigRepository {
    fun getAllConfigs(): Flow<List<ProviderConfig>>
    suspend fun getConfig(provider: AiProvider): ProviderConfig?
    suspend fun saveConfig(config: ProviderConfig)
    fun getEnabledConfigs(): Flow<List<ProviderConfig>>
}

interface SettingsRepository {
    suspend fun setString(key: String, value: String)
    suspend fun getString(key: String): String?
    fun observeString(key: String): Flow<String?>
}

interface ContentSafetyRepository {
    suspend fun assertSafe(
        text: String,
        source: ContentSafetySource,
        preferredConfig: ProviderConfig? = null
    )
}

interface RecommendationRepository {
    fun getRecommendations(limit: Int = 3): Flow<List<TopicRecommendation>>
    fun observeMbti(): Flow<MBTIType?>
    suspend fun setMbti(type: MBTIType?)
    fun observeFeedback(sessionId: String): Flow<RecommendationFeedback?>
    suspend fun recordFeedback(
        sessionId: String,
        topicTitle: String,
        category: String,
        sentiment: RecommendationFeedbackSentiment,
        reasonType: RecommendationFeedbackReasonType
    )
}
