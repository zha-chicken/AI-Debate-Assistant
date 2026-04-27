package com.aidebate.di

import com.aidebate.data.debate.DebateOrchestratorImpl
import com.aidebate.data.repository.*
import com.aidebate.domain.debate.DebateOrchestrator
import com.aidebate.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindDebateRepository(impl: DebateRepositoryImpl): DebateRepository

    @Binds @Singleton
    abstract fun bindTopicRepository(impl: TopicRepositoryImpl): TopicRepository

    @Binds @Singleton
    abstract fun bindProviderConfigRepository(impl: ProviderConfigRepositoryImpl): ProviderConfigRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindArgumentMapRepository(impl: ArgumentMapRepositoryImpl): ArgumentMapRepository

    @Binds @Singleton
    abstract fun bindRebuttalTrainerRepository(impl: RebuttalTrainerRepositoryImpl): RebuttalTrainerRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OrchestratorModule {

    @Binds
    abstract fun bindDebateOrchestrator(impl: DebateOrchestratorImpl): DebateOrchestrator
}
