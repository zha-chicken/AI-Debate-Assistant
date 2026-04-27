package com.aidebate.di

import android.content.Context
import androidx.room.Room
import com.aidebate.data.local.AppDatabase
import com.aidebate.data.local.dao.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "aidebate.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideDebateSessionDao(db: AppDatabase): DebateSessionDao = db.debateSessionDao()
    @Provides fun provideDebateTurnDao(db: AppDatabase): DebateTurnDao = db.debateTurnDao()
    @Provides fun provideDebateTopicDao(db: AppDatabase): DebateTopicDao = db.debateTopicDao()
    @Provides fun provideProviderConfigDao(db: AppDatabase): ProviderConfigDao = db.providerConfigDao()
    @Provides fun provideDebateResultDao(db: AppDatabase): DebateResultDao = db.debateResultDao()
    @Provides fun provideArgumentNodeDao(db: AppDatabase): ArgumentNodeDao = db.argumentNodeDao()
    @Provides fun provideArgumentEdgeDao(db: AppDatabase): ArgumentEdgeDao = db.argumentEdgeDao()
    @Provides fun provideRebuttalSessionDao(db: AppDatabase): RebuttalSessionDao = db.rebuttalSessionDao()
    @Provides fun provideRebuttalAttemptDao(db: AppDatabase): RebuttalAttemptDao = db.rebuttalAttemptDao()
}
