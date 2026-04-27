package com.aidebate.data.remote.adapter

import com.aidebate.data.remote.dto.anthropic.AnthropicContent
import com.aidebate.data.remote.dto.anthropic.AnthropicMessage
import com.aidebate.data.remote.dto.anthropic.AnthropicMessageRequest
import com.aidebate.data.remote.dto.gemini.GeminiContent
import com.aidebate.data.remote.dto.gemini.GeminiGenerationConfig
import com.aidebate.data.remote.dto.gemini.GeminiGenerateRequest
import com.aidebate.data.remote.dto.gemini.GeminiPart
import com.aidebate.data.remote.dto.openai.OpenAiChatRequest
import com.aidebate.data.remote.dto.openai.OpenAiMessage
import com.aidebate.data.remote.service.AnthropicApiService
import com.aidebate.data.remote.service.GeminiApiService
import com.aidebate.data.remote.service.OpenAiApiService
import com.aidebate.data.remote.service.OpenAiCompatibleApiService
import com.aidebate.domain.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface AiProviderAdapter {
    val provider: AiProvider
    suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        config: ChatConfig,
        providerConfig: ProviderConfig
    ): ChatResult

    suspend fun validate(config: ProviderConfig): Boolean
}

@Singleton
class OpenAiAdapter @Inject constructor(
    private val service: OpenAiApiService
) : AiProviderAdapter {
    override val provider = AiProvider.OPENAI

    override suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        config: ChatConfig,
        providerConfig: ProviderConfig
    ): ChatResult {
        val messages = buildList {
            add(OpenAiMessage("system", systemPrompt))
            addAll(conversationHistory.map { OpenAiMessage(it.role, it.content) })
        }
        val request = OpenAiChatRequest(
            model = config.model.ifBlank { providerConfig.modelName },
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens
        )
        val response = service.chat(request)
        if (!response.isSuccessful) {
            throw ProviderException(response.code(), response.errorBody()?.string() ?: "Unknown error")
        }
        val body = response.body()!!
        val choice = body.choices.firstOrNull()
            ?: throw ProviderException(500, "No response from model")
        return ChatResult(
            content = choice.message.content,
            tokenUsage = body.usage?.let { TokenUsage(it.promptTokens, it.completionTokens) },
            finishReason = choice.finishReason
        )
    }

    override suspend fun validate(config: ProviderConfig): Boolean {
        if (config.apiKey.isBlank()) return false
        return try {
            val testRequest = OpenAiChatRequest(
                model = config.modelName.ifBlank { "gpt-3.5-turbo" },
                messages = listOf(OpenAiMessage("user", "respond with just: ok")),
                maxTokens = 5
            )
            service.chat(testRequest).isSuccessful
        } catch (_: Exception) { false }
    }
}

@Singleton
class AnthropicAdapter @Inject constructor(
    private val service: AnthropicApiService
) : AiProviderAdapter {
    override val provider = AiProvider.ANTHROPIC

    override suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        config: ChatConfig,
        providerConfig: ProviderConfig
    ): ChatResult {
        val messages = conversationHistory.map { msg ->
            AnthropicMessage(
                role = if (msg.role == "assistant") "assistant" else "user",
                content = listOf(AnthropicContent(text = msg.content))
            )
        }
        val request = AnthropicMessageRequest(
            model = config.model.ifBlank { providerConfig.modelName },
            maxTokens = config.maxTokens,
            system = systemPrompt,
            messages = messages,
            temperature = config.temperature
        )
        val response = service.createMessage(
            apiKey = providerConfig.apiKey,
            request = request
        )
        if (!response.isSuccessful) {
            throw ProviderException(response.code(), response.errorBody()?.string() ?: "Unknown error")
        }
        val body = response.body()!!
        return ChatResult(
            content = body.content.firstOrNull()?.text ?: "",
            tokenUsage = body.usage?.let { TokenUsage(it.inputTokens, it.outputTokens) },
            finishReason = null
        )
    }

    override suspend fun validate(config: ProviderConfig): Boolean {
        if (config.apiKey.isBlank()) return false
        return try {
            val request = AnthropicMessageRequest(
                model = config.modelName.ifBlank { "claude-3-haiku-20240307" },
                maxTokens = 5,
                messages = listOf(
                    AnthropicMessage("user", listOf(AnthropicContent(text = "respond with just: ok")))
                )
            )
            service.createMessage(apiKey = config.apiKey, request = request).isSuccessful
        } catch (_: Exception) { false }
    }
}

@Singleton
class GeminiAdapter @Inject constructor(
    private val service: GeminiApiService
) : AiProviderAdapter {
    override val provider = AiProvider.GEMINI

    override suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        config: ChatConfig,
        providerConfig: ProviderConfig
    ): ChatResult {
        val contents = conversationHistory.map { msg ->
            GeminiContent(
                role = if (msg.role == "assistant") "model" else "user",
                parts = listOf(GeminiPart(msg.content))
            )
        }
        val systemInstruction = if (systemPrompt.isNotBlank()) {
            GeminiContent(role = "user", parts = listOf(GeminiPart(systemPrompt)))
        } else null

        val request = GeminiGenerateRequest(
            contents = contents.ifEmpty {
                listOf(GeminiContent("user", listOf(GeminiPart(systemPrompt))))
            },
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = config.temperature,
                maxOutputTokens = config.maxTokens
            )
        )
        val modelName = config.model.ifBlank { providerConfig.modelName }
        val response = service.generateContent(
            model = modelName,
            apiKey = providerConfig.apiKey,
            request = request
        )
        if (!response.isSuccessful) {
            throw ProviderException(response.code(), response.errorBody()?.string() ?: "Unknown error")
        }
        val body = response.body()!!
        val candidate = body.candidates?.firstOrNull()
            ?: throw ProviderException(500, "No response from model")
        return ChatResult(
            content = candidate.content.parts.firstOrNull()?.text ?: "",
            tokenUsage = body.usageMetadata?.let {
                TokenUsage(it.promptTokenCount, it.candidatesTokenCount)
            },
            finishReason = candidate.finishReason
        )
    }

    override suspend fun validate(config: ProviderConfig): Boolean {
        if (config.apiKey.isBlank()) return false
        return try {
            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContent("user", listOf(GeminiPart("respond with just: ok")))
                ),
                generationConfig = GeminiGenerationConfig(maxOutputTokens = 5)
            )
            service.generateContent(
                model = config.modelName.ifBlank { "gemini-1.5-flash" },
                apiKey = config.apiKey,
                request = request
            ).isSuccessful
        } catch (_: Exception) { false }
    }
}

@Singleton
class OpenAiCompatibleAdapter @Inject constructor(
    private val moshiConverterFactory: MoshiConverterFactory,
    private val baseOkHttpClient: OkHttpClient
) : AiProviderAdapter {
    override val provider = AiProvider.DEEPSEEK // handled by factory for any OpenAI-compatible

    private val serviceCache = ConcurrentHashMap<String, OpenAiCompatibleApiService>()

    private fun getService(baseUrl: String, apiKey: String): OpenAiCompatibleApiService {
        val normalizedUrl = baseUrl.trimEnd('/') + "/"
        val cacheKey = "$normalizedUrl|$apiKey"
        return serviceCache.getOrPut(cacheKey) {
            val client = baseOkHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .header("Authorization", "Bearer $apiKey")
                        .build()
                    chain.proceed(newRequest)
                }
                .build()
            Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(moshiConverterFactory)
                .build()
                .create(OpenAiCompatibleApiService::class.java)
        }
    }

    override suspend fun chat(
        systemPrompt: String,
        conversationHistory: List<ChatMessage>,
        config: ChatConfig,
        providerConfig: ProviderConfig
    ): ChatResult {
        val api = getService(providerConfig.baseUrl, providerConfig.apiKey)
        val messages = buildList {
            add(OpenAiMessage("system", systemPrompt))
            addAll(conversationHistory.map { OpenAiMessage(it.role, it.content) })
        }
        val request = OpenAiChatRequest(
            model = config.model.ifBlank { providerConfig.modelName },
            messages = messages,
            temperature = config.temperature,
            maxTokens = config.maxTokens
        )
        val response = api.chat(request)
        if (!response.isSuccessful) {
            throw ProviderException(response.code(), response.errorBody()?.string() ?: "Unknown error")
        }
        val body = response.body()!!
        val choice = body.choices.firstOrNull()
            ?: throw ProviderException(500, "No response from model")
        return ChatResult(
            content = choice.message.content,
            tokenUsage = body.usage?.let { TokenUsage(it.promptTokens, it.completionTokens) },
            finishReason = choice.finishReason
        )
    }

    override suspend fun validate(config: ProviderConfig): Boolean {
        if (config.apiKey.isBlank()) return false
        return try {
            val api = getService(config.baseUrl, config.apiKey)
            val testRequest = OpenAiChatRequest(
                model = config.modelName,
                messages = listOf(OpenAiMessage("user", "respond with just: ok")),
                maxTokens = 5
            )
            api.chat(testRequest).isSuccessful
        } catch (_: Exception) { false }
    }
}

class ProviderException(val statusCode: Int, message: String) : Exception(message)

@Singleton
class ProviderAdapterFactory @Inject constructor(
    private val openAiAdapter: OpenAiAdapter,
    private val anthropicAdapter: AnthropicAdapter,
    private val geminiAdapter: GeminiAdapter,
    private val openAiCompatibleAdapter: OpenAiCompatibleAdapter
) {
    private val map: Map<AiProvider, AiProviderAdapter> = mapOf(
        AiProvider.OPENAI to openAiAdapter,
        AiProvider.ANTHROPIC to anthropicAdapter,
        AiProvider.GEMINI to geminiAdapter,
        AiProvider.DEEPSEEK to openAiCompatibleAdapter,
        AiProvider.GROQ to openAiCompatibleAdapter,
        AiProvider.OLLAMA to openAiCompatibleAdapter
    )

    fun getAdapter(provider: AiProvider): AiProviderAdapter =
        map[provider] ?: throw IllegalArgumentException("No adapter for $provider")
}
