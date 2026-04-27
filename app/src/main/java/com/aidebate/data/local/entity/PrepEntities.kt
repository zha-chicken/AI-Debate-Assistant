package com.aidebate.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "argument_nodes",
    indices = [Index("topic_id"), Index("parent_id")])
data class ArgumentNodeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    val type: String,
    val title: String,
    val content: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "x_position") val xPosition: Float,
    @ColumnInfo(name = "y_position") val yPosition: Float,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "argument_edges",
    indices = [Index("topic_id")],
    foreignKeys = [
        ForeignKey(entity = ArgumentNodeEntity::class,
            parentColumns = ["id"], childColumns = ["from_node_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ArgumentNodeEntity::class,
            parentColumns = ["id"], childColumns = ["to_node_id"], onDelete = ForeignKey.CASCADE)
    ])
data class ArgumentEdgeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    @ColumnInfo(name = "from_node_id") val fromNodeId: String,
    @ColumnInfo(name = "to_node_id") val toNodeId: String,
    val relation: String
)

@Entity(tableName = "rebuttal_sessions",
    indices = [Index("topic_id")])
data class RebuttalSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "topic_id") val topicId: String,
    @ColumnInfo(name = "topic_title") val topicTitle: String,
    @ColumnInfo(name = "user_side") val userSide: String,
    val difficulty: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "rebuttal_attempts",
    indices = [Index("session_id")],
    foreignKeys = [
        ForeignKey(entity = RebuttalSessionEntity::class,
            parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)
    ])
data class RebuttalAttemptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "prompt_argument") val promptArgument: String,
    @ColumnInfo(name = "user_response") val userResponse: String,
    @ColumnInfo(name = "time_limit_sec") val timeLimitSec: Int,
    @ColumnInfo(name = "time_taken_ms") val timeTakenMs: Long,
    @ColumnInfo(name = "logic_score") val logicScore: Int,
    @ColumnInfo(name = "clarity_score") val clarityScore: Int,
    @ColumnInfo(name = "persuasion_score") val persuasionScore: Int,
    @ColumnInfo(name = "evidence_score") val evidenceScore: Int,
    @ColumnInfo(name = "total_score") val totalScore: Int,
    val feedback: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
