package com.aidebate.data.local.dao

import androidx.room.*
import com.aidebate.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArgumentNodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: ArgumentNodeEntity)

    @Update
    suspend fun update(node: ArgumentNodeEntity)

    @Query("SELECT * FROM argument_nodes WHERE topic_id = :topicId ORDER BY created_at ASC")
    fun getByTopicId(topicId: String): Flow<List<ArgumentNodeEntity>>

    @Query("DELETE FROM argument_nodes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM argument_nodes WHERE topic_id = :topicId")
    suspend fun deleteByTopicId(topicId: String)
}

@Dao
interface ArgumentEdgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(edge: ArgumentEdgeEntity)

    @Query("SELECT * FROM argument_edges WHERE topic_id = :topicId")
    fun getByTopicId(topicId: String): Flow<List<ArgumentEdgeEntity>>

    @Query("DELETE FROM argument_edges WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM argument_edges WHERE topic_id = :topicId")
    suspend fun deleteByTopicId(topicId: String)
}

@Dao
interface RebuttalSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RebuttalSessionEntity)

    @Query("SELECT * FROM rebuttal_sessions ORDER BY created_at DESC")
    fun getAll(): Flow<List<RebuttalSessionEntity>>

    @Query("SELECT * FROM rebuttal_sessions WHERE id = :id")
    suspend fun getById(id: String): RebuttalSessionEntity?

    @Query("DELETE FROM rebuttal_sessions WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface RebuttalAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: RebuttalAttemptEntity)

    @Query("SELECT * FROM rebuttal_attempts WHERE session_id = :sessionId ORDER BY created_at DESC")
    fun getBySessionId(sessionId: String): Flow<List<RebuttalAttemptEntity>>

    @Query("SELECT * FROM rebuttal_attempts ORDER BY created_at DESC")
    fun getAll(): Flow<List<RebuttalAttemptEntity>>
}
