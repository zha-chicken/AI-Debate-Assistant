package com.aidebate.data.repository

import com.aidebate.data.local.dao.DebateTopicDao
import com.aidebate.data.local.mapper.toDomain
import com.aidebate.data.local.mapper.toEntity
import com.aidebate.domain.model.DebateTopic
import com.aidebate.domain.repository.TopicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TopicRepositoryImpl @Inject constructor(
    private val topicDao: DebateTopicDao
) : TopicRepository {

    override fun getAllTopics(): Flow<List<DebateTopic>> =
        topicDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun saveTopic(topic: DebateTopic) {
        topicDao.insert(topic.toEntity())
    }

    override suspend fun getTopic(topicId: String): DebateTopic? =
        topicDao.getById(topicId)?.toDomain()

    override suspend fun seedPredefinedTopics() {
        val existingTitles = topicDao.getAllTitles()
            .map(::normalizeTitle)
            .toSet()

        DefaultTopicLibrary.topics
            .filterNot { normalizeTitle(it.title) in existingTitles }
            .map {
                DebateTopic(
                    title = it.title,
                    category = it.category,
                    description = it.description,
                    isPredefined = true
                )
            }
            .forEach { topicDao.insert(it.toEntity()) }
    }

    private fun normalizeTitle(title: String): String =
        title.trim().trimEnd('?').lowercase()
}
