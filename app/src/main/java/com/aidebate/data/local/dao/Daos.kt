package com.aidebate.data.local.dao

import androidx.room.*
import com.aidebate.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DebateSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: DebateSessionEntity)

    @Update
    suspend fun update(session: DebateSessionEntity)

    @Query("SELECT * FROM debate_sessions WHERE id = :id")
    fun getById(id: String): Flow<DebateSessionEntity?>

    @Query("""
        SELECT ds.id, ds.mode, ds.format, ds.status, ds.user_side as userSide, ds.created_at as createdAt,
               dt.title as topicTitle, (SELECT COUNT(*) FROM debate_turns WHERE session_id = ds.id) as turnCount
        FROM debate_sessions ds
        INNER JOIN debate_topics dt ON ds.topic_id = dt.id
        ORDER BY ds.created_at DESC
    """)
    fun getAllSummaries(): Flow<List<SessionSummaryTuple>>

    @Query("DELETE FROM debate_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}

data class SessionSummaryTuple(
    val id: String,
    val topicTitle: String,
    val mode: String,
    val format: String,
    val status: String,
    val userSide: String?,
    val turnCount: Int,
    val createdAt: Long
)

@Dao
interface DebateTurnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(turn: DebateTurnEntity)

    @Query("SELECT * FROM debate_turns WHERE session_id = :sessionId ORDER BY turn_index ASC")
    fun getBySessionId(sessionId: String): Flow<List<DebateTurnEntity>>

    @Query("DELETE FROM debate_turns WHERE session_id = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}

@Dao
interface DebateTopicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(topic: DebateTopicEntity)

    @Query("SELECT * FROM debate_topics ORDER BY is_predefined DESC, created_at DESC")
    fun getAll(): Flow<List<DebateTopicEntity>>

    @Query("SELECT * FROM debate_topics WHERE id = :id")
    suspend fun getById(id: String): DebateTopicEntity?

    @Query("SELECT COUNT(*) FROM debate_topics")
    suspend fun count(): Int

    @Query("SELECT title FROM debate_topics")
    suspend fun getAllTitles(): List<String>
}

@Dao
interface ProviderConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ProviderConfigEntity)

    @Query("SELECT * FROM provider_configs")
    fun getAll(): Flow<List<ProviderConfigEntity>>

    @Query("SELECT * FROM provider_configs WHERE provider_id = :id")
    suspend fun getById(id: String): ProviderConfigEntity?

    @Query("SELECT * FROM provider_configs WHERE is_enabled = 1")
    fun getEnabled(): Flow<List<ProviderConfigEntity>>
}

@Dao
interface DebateResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: DebateResultEntity)

    @Query("SELECT * FROM debate_results WHERE session_id = :sessionId")
    fun getBySessionId(sessionId: String): Flow<DebateResultEntity?>

    @Query("SELECT * FROM debate_results ORDER BY created_at DESC")
    fun getAll(): Flow<List<DebateResultEntity>>
}
