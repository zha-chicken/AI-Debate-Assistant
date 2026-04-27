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
        if (topicDao.count() > 0) return

        val topics = listOf(
            DebateTopic(title = "AI should be regulated by governments", category = "Technology", description = "Should artificial intelligence development and deployment be subject to strict government oversight and regulation?", isPredefined = true),
            DebateTopic(title = "Social media does more harm than good", category = "Technology", description = "Do the negative effects of social media on society outweigh the benefits?", isPredefined = true),
            DebateTopic(title = "Cryptocurrency is the future of finance", category = "Technology", description = "Will decentralized digital currencies replace traditional financial systems?", isPredefined = true),
            DebateTopic(title = "Free will is an illusion", category = "Philosophy", description = "Are our choices truly free, or are they predetermined by prior causes?", isPredefined = true),
            DebateTopic(title = "Morality is subjective", category = "Philosophy", description = "Are moral truths objective, or do they depend on individual or cultural perspectives?", isPredefined = true),
            DebateTopic(title = "The ends justify the means", category = "Philosophy", description = "Can good outcomes morally justify harmful or unethical actions taken to achieve them?", isPredefined = true),
            DebateTopic(title = "We live in a simulation", category = "Science", description = "Is it possible — or even likely — that our reality is a computer simulation?", isPredefined = true),
            DebateTopic(title = "Genetic engineering of humans should be permitted", category = "Science", description = "Should we allow genetic modification of human embryos for non-medical purposes?", isPredefined = true),
            DebateTopic(title = "Space exploration is worth the cost", category = "Science", description = "Does the scientific and inspirational value of space exploration justify its enormous expense?", isPredefined = true),
            DebateTopic(title = "Universal basic income is necessary", category = "Politics", description = "Should governments provide a guaranteed income to all citizens regardless of employment?", isPredefined = true),
            DebateTopic(title = "Democracy is the best form of government", category = "Politics", description = "Is democracy truly superior to other forms of governance in all contexts?", isPredefined = true),
            DebateTopic(title = "Privacy is more important than security", category = "Politics", description = "Should individual privacy rights take precedence over collective security measures?", isPredefined = true),
            DebateTopic(title = "College education should be free", category = "Education", description = "Should higher education be publicly funded and tuition-free for all students?", isPredefined = true),
            DebateTopic(title = "Standardized testing should be abolished", category = "Education", description = "Do standardized tests harm education more than they help measure it?", isPredefined = true),
            DebateTopic(title = "Online learning is as effective as in-person", category = "Education", description = "Can virtual education match or exceed the quality of traditional classroom learning?", isPredefined = true),
            DebateTopic(title = "AI-generated art is not real art", category = "Arts", description = "Does art require human intention and emotion to qualify as genuine art?", isPredefined = true),
            DebateTopic(title = "Video games are a form of art", category = "Arts", description = "Do video games deserve recognition alongside film, literature, and visual arts?", isPredefined = true),
            DebateTopic(title = "Censorship in art is never justified", category = "Arts", description = "Should artistic expression be completely free from any form of censorship?", isPredefined = true),
            DebateTopic(title = "Remote work is better than office work", category = "Technology", description = "Is working from home more productive and fulfilling than working in an office?", isPredefined = true),
            DebateTopic(title = "Nuclear energy is the best solution to climate change", category = "Science", description = "Should nuclear power be the primary strategy for reducing carbon emissions?", isPredefined = true)
        )
        topics.forEach { topicDao.insert(it.toEntity()) }
    }
}
