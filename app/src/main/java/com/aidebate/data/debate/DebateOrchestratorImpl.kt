package com.aidebate.data.debate

import com.aidebate.data.remote.adapter.ProviderAdapterFactory
import com.aidebate.data.remote.adapter.ProviderException
import com.aidebate.domain.debate.DebateOrchestrator
import com.aidebate.domain.debate.DebateStateMachine
import com.aidebate.domain.model.*
import com.aidebate.domain.repository.DebateRepository
import com.aidebate.domain.repository.ProviderConfigRepository
import com.aidebate.domain.repository.TopicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebateOrchestratorImpl @Inject constructor(
    private val debateRepository: DebateRepository,
    private val topicRepository: TopicRepository,
    private val providerConfigRepository: ProviderConfigRepository,
    private val adapterFactory: ProviderAdapterFactory
) : DebateOrchestrator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow<DebateSession?>(null)
    override val session: StateFlow<DebateSession?> = _session.asStateFlow()

    private val _turns = MutableStateFlow<List<DebateTurn>>(emptyList())
    override val turns: StateFlow<List<DebateTurn>> = _turns.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    override val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _contextualState = MutableStateFlow<DebateContextualState>(
        DebateContextualState.WaitingForUserInput
    )
    override val contextualState: StateFlow<DebateContextualState> = _contextualState.asStateFlow()

    private var stateMachine: DebateStateMachine? = null
    private var turnIndex = 0
    private var topicTitle = ""

    override suspend fun initialize(session: DebateSession, initialTurns: List<DebateTurn>) {
        _session.value = session
        _turns.value = initialTurns
        turnIndex = initialTurns.size

        val topic = topicRepository.getTopic(session.topicId)
        topicTitle = topic?.title ?: ""

        stateMachine = DebateStateMachine(session.format).start()

        when {
            session.status != SessionStatus.ACTIVE -> {
                _contextualState.value = DebateContextualState.DebateCompleted
            }
            session.mode == DebateMode.USER_VS_AI -> {
                _contextualState.value = DebateContextualState.WaitingForUserInput
            }
            session.mode == DebateMode.AI_VS_AI -> {
                val firstSpeaker = stateMachine?.currentSpeakerState() ?: SpeakerRole.AI_PROPOSITION
                val provider = if (firstSpeaker == SpeakerRole.AI_PROPOSITION)
                    session.providerProposition else session.providerOpposition
                _contextualState.value = DebateContextualState.WaitingForTap(firstSpeaker, provider)
            }
        }
    }

    override suspend fun submitUserTurn(content: String): DebateTurn {
        val currentSession = _session.value ?: error("No active session")
        val userTurn = DebateTurn(
            id = UUID.randomUUID().toString(),
            sessionId = currentSession.id,
            speakerRole = SpeakerRole.USER,
            content = content,
            phase = stateMachine?.currentPhaseState(),
            turnIndex = turnIndex++
        )
        _turns.value = _turns.value + userTurn
        debateRepository.addTurn(userTurn)

        // AI responds
        _contextualState.value = DebateContextualState.WaitingForAiTurn(
            SpeakerRole.AI_OPPOSITION,
            currentSession.providerOpposition,
            currentSession.modelOpposition
        )
        _isThinking.value = true

        try {
            val aiTurn = generateAiResponse(
                speakerRole = SpeakerRole.AI_OPPOSITION,
                provider = currentSession.providerOpposition,
                model = currentSession.modelOpposition
            )
            _turns.value = _turns.value + aiTurn
            debateRepository.addTurn(aiTurn)
            turnIndex++

            val result = stateMachine?.advance()
            if (result is DebateStateMachine.AdvanceResult.Completed) {
                completeDebate(currentSession)
            } else {
                _contextualState.value = DebateContextualState.WaitingForUserInput
            }
            return aiTurn
        } catch (e: ProviderException) {
            _contextualState.value = DebateContextualState.Error(
                "API error: ${e.message} (${e.statusCode})"
            )
            throw e
        } catch (e: Exception) {
            _contextualState.value = DebateContextualState.Error(
                "Network error: ${e.message}"
            )
            throw e
        } finally {
            _isThinking.value = false
        }
    }

    override suspend fun advanceAiTurn(): DebateTurn {
        val currentSession = _session.value ?: error("No active session")
        val result = stateMachine?.advance()
        if (result is DebateStateMachine.AdvanceResult.Completed) {
            completeDebate(currentSession)
            error("Debate already completed")
        }

        val nextResult = result as? DebateStateMachine.AdvanceResult.NextTurn
            ?: error("Unexpected state")
        val speakerRole = nextResult.speaker
        val provider = if (speakerRole == SpeakerRole.AI_PROPOSITION)
            currentSession.providerProposition else currentSession.providerOpposition
        val model = if (speakerRole == SpeakerRole.AI_PROPOSITION)
            currentSession.modelProposition else currentSession.modelOpposition

        _contextualState.value = DebateContextualState.WaitingForAiTurn(speakerRole, provider, model)
        _isThinking.value = true

        try {
            val aiTurn = generateAiResponse(speakerRole, provider, model)
            _turns.value = _turns.value + aiTurn
            debateRepository.addTurn(aiTurn)
            turnIndex++

            // Check what's next
            val nextState = stateMachine?.advance()
            when (nextState) {
                is DebateStateMachine.AdvanceResult.Completed -> {
                    completeDebate(currentSession)
                }
                is DebateStateMachine.AdvanceResult.NextTurn -> {
                    val nextProvider = if (nextState.speaker == SpeakerRole.AI_PROPOSITION)
                        currentSession.providerProposition else currentSession.providerOpposition
                    _contextualState.value = DebateContextualState.WaitingForTap(
                        nextState.speaker, nextProvider
                    )
                }
                null -> {}
            }
            return aiTurn
        } catch (e: ProviderException) {
            _contextualState.value = DebateContextualState.Error(
                "API error: ${e.message} (${e.statusCode})"
            )
            throw e
        } catch (e: Exception) {
            _contextualState.value = DebateContextualState.Error(
                "Network error: ${e.message}"
            )
            throw e
        } finally {
            _isThinking.value = false
        }
    }

    override suspend fun requestJudgment(): DebateResult {
        val currentSession = _session.value ?: error("No active session")
        _contextualState.value = DebateContextualState.Judging
        _isThinking.value = true

        try {
            val transcript = buildTranscript()
            val judgePrompt = buildString {
                append("You are an impartial debate judge. Read the following debate transcript ")
                append("and determine the winner. Consider argument quality, evidence, logic, and persuasiveness.\n\n")
                append("Topic: $topicTitle\n\n")
                append("=== DEBATE TRANSCRIPT ===\n")
                append(transcript)
                append("\n=== END TRANSCRIPT ===\n\n")
                append("Respond in this format:\n")
                append("WINNER: [PROPOSITION or OPPOSITION]\n")
                append("SUMMARY: [2-3 sentence explanation of why this side won]")
            }

            val judgeProvider = AiProvider.OPENAI
            val judgeConfig = providerConfigRepository.getConfig(judgeProvider)
                ?: throw ProviderException(0, "No judge provider configured")
            val adapter = adapterFactory.getAdapter(judgeProvider)
            val result = adapter.chat(
                systemPrompt = "You are an impartial and fair debate judge.",
                conversationHistory = listOf(ChatMessage("user", judgePrompt)),
                config = ChatConfig(model = judgeConfig.modelName.ifBlank { "gpt-3.5-turbo" }),
                providerConfig = judgeConfig
            )

            val (winner, summary) = parseJudgeResponse(result.content)
            val debateResult = DebateResult(
                id = UUID.randomUUID().toString(),
                sessionId = currentSession.id,
                winner = winner,
                summary = summary,
                judgedByProvider = judgeProvider,
                judgedByModel = judgeConfig.modelName
            )
            debateRepository.saveResult(debateResult)
            _contextualState.value = DebateContextualState.DebateCompleted
            return debateResult
        } catch (e: Exception) {
            _contextualState.value = DebateContextualState.DebateCompleted
            throw e
        } finally {
            _isThinking.value = false
        }
    }

    override fun buildConversationContext(): List<ChatMessage> {
        val currentSession = _session.value ?: return emptyList()
        val allTurns = _turns.value
        return allTurns.map { turn ->
            val role = when (turn.speakerRole) {
                SpeakerRole.USER -> "user"
                SpeakerRole.AI_PROPOSITION -> "assistant"
                SpeakerRole.AI_OPPOSITION -> "assistant"
                SpeakerRole.MODERATOR -> "user"
            }
            ChatMessage(role, turn.content)
        }
    }

    override suspend fun endDebate() {
        val currentSession = _session.value ?: return
        completeDebate(currentSession)
    }

    private suspend fun completeDebate(session: DebateSession) {
        val completed = session.copy(status = SessionStatus.COMPLETED)
        _session.value = completed
        debateRepository.updateSession(completed)
        _contextualState.value = DebateContextualState.DebateCompleted
    }

    private suspend fun generateAiResponse(
        speakerRole: SpeakerRole,
        provider: AiProvider,
        model: String
    ): DebateTurn {
        val currentSession = _session.value ?: error("No active session")
        val config = providerConfigRepository.getConfig(provider)
            ?: throw ProviderException(0, "No config for provider $provider")

        val adapter = adapterFactory.getAdapter(provider)
        val systemPrompt = buildSystemPrompt(speakerRole)
        val history = buildConversationContext()

        val result = adapter.chat(
            systemPrompt = systemPrompt,
            conversationHistory = history,
            config = ChatConfig(model = model.ifBlank { config.modelName }),
            providerConfig = config
        )

        return DebateTurn(
            id = UUID.randomUUID().toString(),
            sessionId = currentSession.id,
            speakerRole = speakerRole,
            content = result.content,
            phase = stateMachine?.currentPhaseState(),
            turnIndex = turnIndex,
            providerUsed = provider,
            modelUsed = model
        )
    }

    private fun buildSystemPrompt(role: SpeakerRole): String {
        val phase = stateMachine?.currentPhaseState()
        val side = when (role) {
            SpeakerRole.AI_PROPOSITION -> "FOR"
            SpeakerRole.AI_OPPOSITION -> "AGAINST"
            else -> "NEUTRAL"
        }
        return buildString {
            append("You are an expert debater arguing $side the topic: \"$topicTitle\".\n")
            if (phase != null) {
                append("This is the ${phase.name} phase. ")
                when (phase) {
                    StructuredPhase.OPENING -> append("Present your main arguments clearly and persuasively.")
                    StructuredPhase.REBUTTAL -> append("Address your opponent's points directly and counter them with logic and evidence.")
                    StructuredPhase.CLOSING -> append("Summarize your strongest points and make a compelling final case.")
                }
            } else {
                append("Respond naturally to the opponent's arguments. Be persuasive, logical, and civil.")
            }
            append("\nKeep your response under 250 words. Be concise but impactful.")
        }
    }

    private fun buildTranscript(): String {
        return _turns.value.joinToString("\n\n") { turn ->
            "${turn.speakerRole.name}: ${turn.content}"
        }
    }

    private fun parseJudgeResponse(response: String): Pair<SpeakerRole?, String> {
        val winner = when {
            response.contains("WINNER: PROPOSITION", ignoreCase = true) ||
            response.contains("WINNER: FOR", ignoreCase = true) -> SpeakerRole.AI_PROPOSITION
            response.contains("WINNER: OPPOSITION", ignoreCase = true) ||
            response.contains("WINNER: AGAINST", ignoreCase = true) -> SpeakerRole.AI_OPPOSITION
            else -> null
        }
        val summary = response
            .replace(Regex("WINNER:.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("SUMMARY:", RegexOption.IGNORE_CASE), "")
            .trim()
        return winner to summary
    }
}
