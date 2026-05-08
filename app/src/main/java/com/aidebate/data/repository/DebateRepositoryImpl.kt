package com.aidebate.data.repository

import com.aidebate.data.local.dao.DebateResultDao
import com.aidebate.data.local.dao.DebateSessionDao
import com.aidebate.data.local.dao.DebateTurnDao
import com.aidebate.data.local.mapper.*
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.DebateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebateRepositoryImpl @Inject constructor(
    private val sessionDao: DebateSessionDao,
    private val turnDao: DebateTurnDao,
    private val resultDao: DebateResultDao
) : DebateRepository {

    override suspend fun createSession(session: DebateSession) {
        sessionDao.insert(session.toEntity())
    }

    override suspend fun updateSession(session: DebateSession) {
        sessionDao.update(session.toEntity())
    }

    override fun getSession(sessionId: String): Flow<DebateSession?> =
        sessionDao.getById(sessionId).map { it?.toDomain() }

    override suspend fun addTurn(turn: DebateTurn) {
        turnDao.insert(turn.toEntity())
    }

    override suspend fun updateTurn(turn: DebateTurn) {
        turnDao.insert(turn.toEntity())
    }
    override fun getTurns(sessionId: String): Flow<List<DebateTurn>> =
        turnDao.getBySessionId(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun saveResult(result: DebateResult) {
        resultDao.insert(result.toEntity())
    }

    override fun getResult(sessionId: String): Flow<DebateResult?> =
        resultDao.getBySessionId(sessionId).map { it?.toDomain() }

    override fun getAllResults(): Flow<List<DebateResult>> =
        resultDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getAllSessions(): Flow<List<DebateSessionSummary>> =
        sessionDao.getAllSummaries().map { list -> list.map { it.toDomain() } }

    override suspend fun deleteSession(sessionId: String) {
        turnDao.deleteBySessionId(sessionId)
        sessionDao.deleteById(sessionId)
    }
}
