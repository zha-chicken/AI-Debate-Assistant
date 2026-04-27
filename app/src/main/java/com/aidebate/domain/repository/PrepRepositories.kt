package com.aidebate.domain.repository

import com.aidebate.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ArgumentMapRepository {
    fun getNodes(topicId: String): Flow<List<ArgumentNode>>
    fun getEdges(topicId: String): Flow<List<ArgumentEdge>>
    suspend fun saveNode(node: ArgumentNode)
    suspend fun updateNode(node: ArgumentNode)
    suspend fun deleteNode(nodeId: String)
    suspend fun saveEdge(edge: ArgumentEdge)
    suspend fun deleteEdge(edgeId: String)
    suspend fun deleteAllForTopic(topicId: String)
}

interface RebuttalTrainerRepository {
    fun getAllSessions(): Flow<List<RebuttalSession>>
    suspend fun createSession(session: RebuttalSession)
    suspend fun getSession(sessionId: String): RebuttalSession?
    fun getAttempts(sessionId: String): Flow<List<RebuttalAttempt>>
    suspend fun saveAttempt(attempt: RebuttalAttempt)
}
