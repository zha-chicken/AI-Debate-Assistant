package com.aidebate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aidebate.data.local.dao.*
import com.aidebate.data.local.entity.*

@Database(
    entities = [
        DebateSessionEntity::class,
        DebateTurnEntity::class,
        DebateTopicEntity::class,
        ProviderConfigEntity::class,
        DebateResultEntity::class,
        ArgumentNodeEntity::class,
        ArgumentEdgeEntity::class,
        RebuttalSessionEntity::class,
        RebuttalAttemptEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun debateSessionDao(): DebateSessionDao
    abstract fun debateTurnDao(): DebateTurnDao
    abstract fun debateTopicDao(): DebateTopicDao
    abstract fun providerConfigDao(): ProviderConfigDao
    abstract fun debateResultDao(): DebateResultDao
    abstract fun argumentNodeDao(): ArgumentNodeDao
    abstract fun argumentEdgeDao(): ArgumentEdgeDao
    abstract fun rebuttalSessionDao(): RebuttalSessionDao
    abstract fun rebuttalAttemptDao(): RebuttalAttemptDao
}
