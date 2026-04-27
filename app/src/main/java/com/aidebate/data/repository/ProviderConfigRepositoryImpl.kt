package com.aidebate.data.repository

import com.aidebate.data.local.dao.ProviderConfigDao
import com.aidebate.data.local.mapper.toDomain
import com.aidebate.data.local.mapper.toEntity
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.ProviderConfig
import com.aidebate.domain.repository.ProviderConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderConfigRepositoryImpl @Inject constructor(
    private val configDao: ProviderConfigDao
) : ProviderConfigRepository {

    override fun getAllConfigs(): Flow<List<ProviderConfig>> =
        configDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getConfig(provider: AiProvider): ProviderConfig? =
        configDao.getById(provider.name)?.toDomain()

    override suspend fun saveConfig(config: ProviderConfig) {
        configDao.insert(config.toEntity())
    }

    override fun getEnabledConfigs(): Flow<List<ProviderConfig>> =
        configDao.getEnabled().map { list -> list.map { it.toDomain() } }
}
