package com.aidebate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debate_sessions")
data class DebateSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    val mode: String,
    val format: String,
    @ColumnInfo(name = "user_side") val userSide: String?,
    @ColumnInfo(name = "provider_proposition") val providerProposition: String,
    @ColumnInfo(name = "provider_opposition") val providerOpposition: String,
    @ColumnInfo(name = "model_proposition") val modelProposition: String,
    @ColumnInfo(name = "model_opposition") val modelOpposition: String,
    val status: String,
    @ColumnInfo(name = "current_phase") val currentPhase: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "debate_turns")
data class DebateTurnEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "speaker_role") val speakerRole: String,
    val content: String,
    val phase: String?,
    @ColumnInfo(name = "turn_index") val turnIndex: Int,
    @ColumnInfo(name = "provider_used") val providerUsed: String?,
    @ColumnInfo(name = "model_used") val modelUsed: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "debate_topics")
data class DebateTopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val description: String,
    @ColumnInfo(name = "is_predefined") val isPredefined: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "provider_configs")
data class ProviderConfigEntity(
    @PrimaryKey @ColumnInfo(name = "provider_id") val providerId: String,
    @ColumnInfo(name = "api_key") val apiKey: String,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "base_url") val baseUrl: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean
)

@Entity(tableName = "debate_results")
data class DebateResultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    val winner: String?,
    val summary: String,
    @ColumnInfo(name = "judged_by_provider") val judgedByProvider: String?,
    @ColumnInfo(name = "judged_by_model") val judgedByModel: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
