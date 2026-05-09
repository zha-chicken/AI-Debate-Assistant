package com.aidebate.data.repository

import com.aidebate.data.remote.dto.openai.OpenAiModerationRequest
import com.aidebate.data.remote.service.OpenAiApiService
import com.aidebate.domain.model.AiProvider
import com.aidebate.domain.model.ContentSafetyException
import com.aidebate.domain.model.ContentSafetyResult
import com.aidebate.domain.model.ContentSafetySource
import com.aidebate.domain.repository.ContentSafetyRepository
import com.aidebate.domain.repository.ProviderConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentSafetyRepositoryImpl @Inject constructor(
    private val openAiApiService: OpenAiApiService,
    private val providerConfigRepository: ProviderConfigRepository
) : ContentSafetyRepository {

    override suspend fun assertSafe(text: String, source: ContentSafetySource) {
        val normalized = text.trim()
        if (normalized.isBlank()) return

        val localReasons = detectLocalPolicyViolations(normalized)
        val moderationReasons = checkOpenAiModeration(normalized)
        val reasons = (localReasons + moderationReasons).distinct()

        if (reasons.isNotEmpty()) {
            throw ContentSafetyException(
                result = ContentSafetyResult(isAllowed = false, reasons = reasons),
                source = source
            )
        }
    }

    private suspend fun checkOpenAiModeration(text: String): List<String> {
        val config = providerConfigRepository.getConfig(AiProvider.OPENAI)
        if (config == null || !config.isEnabled || config.apiKey.isBlank()) {
            return listOf("内容安全检测不可用，请先配置 OpenAI API Key")
        }

        val reasons = mutableSetOf<String>()
        text.chunked(MAX_MODERATION_CHARS).forEach { chunk ->
            val response = openAiApiService.moderate(
                authorization = "Bearer ${config.apiKey}",
                request = OpenAiModerationRequest(input = chunk)
            )
            if (!response.isSuccessful) {
                reasons += "内容安全检测失败"
                return@forEach
            }
            val result = response.body()?.results?.firstOrNull() ?: run {
                reasons += "内容安全检测失败"
                return@forEach
            }
            if (result.flagged) {
                reasons += "安全策略命中"
            }
            result.categories
                .filterValues { it }
                .keys
                .mapNotNull { categoryLabel(it) }
                .forEach { reasons += it }
        }
        return reasons.toList()
    }

    private fun detectLocalPolicyViolations(text: String): List<String> {
        val lower = text.lowercase()
        val reasons = mutableListOf<String>()

        if (POLITICAL_SENSITIVE_KEYWORDS.any { it in lower }) {
            reasons += "政治敏感内容"
        }
        if (DANGEROUS_INSTRUCTION_KEYWORDS.any { it in lower }) {
            reasons += "危险物品制作方法"
        }
        if (SEXUAL_KEYWORDS.any { it in lower }) {
            reasons += "色情内容"
        }
        if (HATE_KEYWORDS.any { it in lower }) {
            reasons += "仇恨言论"
        }

        return reasons
    }

    private fun categoryLabel(category: String): String? {
        return when {
            category.startsWith("hate") -> "仇恨言论"
            category.startsWith("sexual") -> "色情内容"
            category.startsWith("illicit") -> "危险或非法行为"
            category.startsWith("violence") -> "暴力危险内容"
            category.startsWith("harassment") -> "骚扰或攻击性内容"
            category.startsWith("self-harm") -> "自伤风险内容"
            else -> null
        }
    }

    private companion object {
        private const val MAX_MODERATION_CHARS = 12_000

        private val POLITICAL_SENSITIVE_KEYWORDS = listOf(
            "习近平", "中共", "共产党", "六四", "天安门事件", "台湾独立", "港独", "藏独", "疆独",
            "法轮功", "政治敏感", "ccp", "xi jinping", "tiananmen", "taiwan independence",
            "hong kong independence", "uyghur", "xinjiang", "tibet independence"
        )

        private val DANGEROUS_INSTRUCTION_KEYWORDS = listOf(
            "制作炸弹", "做炸弹", "炸药配方", "爆炸物制作", "制作枪", "自制枪", "毒气制作",
            "燃烧瓶制作", "how to make a bomb", "make a bomb", "bomb recipe", "homemade explosive",
            "build a gun", "make a gun", "napalm recipe", "make poison gas"
        )

        private val SEXUAL_KEYWORDS = listOf(
            "色情", "性爱", "裸聊", "黄片", "成人内容", "sexually explicit", "porn", "nude chat"
        )

        private val HATE_KEYWORDS = listOf(
            "种族灭绝", "劣等民族", "消灭某族", "racial slur", "inferior race", "exterminate"
        )
    }
}
