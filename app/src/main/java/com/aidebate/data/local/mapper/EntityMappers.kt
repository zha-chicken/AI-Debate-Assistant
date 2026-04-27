package com.aidebate.data.local.mapper

import com.aidebate.data.local.dao.SessionSummaryTuple
import com.aidebate.data.local.entity.*
import com.aidebate.domain.model.*

fun DebateSession.toEntity() = DebateSessionEntity(
    id = id, topicId = topicId, mode = mode.name, format = format.name,
    userSide = userSide?.name, providerProposition = providerProposition.name,
    providerOpposition = providerOpposition.name,
    modelProposition = modelProposition, modelOpposition = modelOpposition,
    status = status.name, currentPhase = currentPhase?.name, createdAt = createdAt
)

fun DebateSessionEntity.toDomain() = DebateSession(
    id = id, topicId = topicId, mode = DebateMode.valueOf(mode),
    format = DebateFormat.valueOf(format),
    userSide = userSide?.let { SpeakerRole.valueOf(it) },
    providerProposition = AiProvider.valueOf(providerProposition),
    providerOpposition = AiProvider.valueOf(providerOpposition),
    modelProposition = modelProposition, modelOpposition = modelOpposition,
    status = SessionStatus.valueOf(status),
    currentPhase = currentPhase?.let { StructuredPhase.valueOf(it) }, createdAt = createdAt
)

fun DebateTurn.toEntity() = DebateTurnEntity(
    id = id, sessionId = sessionId, speakerRole = speakerRole.name,
    content = content, phase = phase?.name, turnIndex = turnIndex,
    providerUsed = providerUsed?.name, modelUsed = modelUsed, createdAt = createdAt
)

fun DebateTurnEntity.toDomain() = DebateTurn(
    id = id, sessionId = sessionId, speakerRole = SpeakerRole.valueOf(speakerRole),
    content = content, phase = phase?.let { StructuredPhase.valueOf(it) },
    turnIndex = turnIndex, providerUsed = providerUsed?.let { AiProvider.valueOf(it) },
    modelUsed = modelUsed, createdAt = createdAt
)

fun DebateTopic.toEntity() = DebateTopicEntity(
    id = id, title = title, category = category, description = description,
    isPredefined = isPredefined, createdAt = createdAt
)

fun DebateTopicEntity.toDomain() = DebateTopic(
    id = id, title = title, category = category, description = description,
    isPredefined = isPredefined, createdAt = createdAt
)

fun ProviderConfig.toEntity() = ProviderConfigEntity(
    providerId = provider.name, apiKey = apiKey, modelName = modelName,
    baseUrl = baseUrl, isEnabled = isEnabled
)

fun ProviderConfigEntity.toDomain() = ProviderConfig(
    provider = AiProvider.valueOf(providerId), apiKey = apiKey,
    modelName = modelName, baseUrl = baseUrl, isEnabled = isEnabled
)

fun DebateResult.toEntity() = DebateResultEntity(
    id = id, sessionId = sessionId, winner = winner?.name,
    summary = summary, judgedByProvider = judgedByProvider?.name,
    judgedByModel = judgedByModel, createdAt = createdAt
)

fun DebateResultEntity.toDomain() = DebateResult(
    id = id, sessionId = sessionId, winner = winner?.let { SpeakerRole.valueOf(it) },
    summary = summary, judgedByProvider = judgedByProvider?.let { AiProvider.valueOf(it) },
    judgedByModel = judgedByModel, createdAt = createdAt
)

fun SessionSummaryTuple.toDomain() = DebateSessionSummary(
    id = id, topicTitle = topicTitle, mode = DebateMode.valueOf(mode),
    format = DebateFormat.valueOf(format), status = SessionStatus.valueOf(status),
    turnCount = turnCount, createdAt = createdAt
)
