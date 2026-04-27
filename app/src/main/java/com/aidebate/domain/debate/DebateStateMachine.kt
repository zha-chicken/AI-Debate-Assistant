package com.aidebate.domain.debate

import com.aidebate.domain.model.DebateFormat
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.domain.model.StructuredPhase

class DebateStateMachine(private val format: DebateFormat) {

    private var currentPhase: StructuredPhase? = null
    private var currentSpeaker: SpeakerRole = SpeakerRole.AI_PROPOSITION

    fun currentPhaseState(): StructuredPhase? = currentPhase
    fun currentSpeakerState(): SpeakerRole = currentSpeaker

    fun start(): DebateStateMachine {
        when (format) {
            DebateFormat.STRUCTURED -> {
                currentPhase = StructuredPhase.OPENING
                currentSpeaker = SpeakerRole.AI_PROPOSITION
            }
            DebateFormat.FREE_FLOW -> {
                currentPhase = null
                currentSpeaker = SpeakerRole.AI_PROPOSITION
            }
        }
        return this
    }

    fun advance(): AdvanceResult {
        when (format) {
            DebateFormat.STRUCTURED -> return advanceStructured()
            DebateFormat.FREE_FLOW -> return advanceFreeFlow()
        }
    }

    private fun advanceStructured(): AdvanceResult {
        val phase = currentPhase ?: StructuredPhase.OPENING
        val nextTurn = when (phase) {
            StructuredPhase.OPENING -> {
                when (currentSpeaker) {
                    SpeakerRole.AI_PROPOSITION -> {
                        currentSpeaker = SpeakerRole.AI_OPPOSITION
                        AdvanceResult.NextTurn(SpeakerRole.AI_OPPOSITION, phase)
                    }
                    SpeakerRole.AI_OPPOSITION -> {
                        currentPhase = StructuredPhase.REBUTTAL
                        currentSpeaker = SpeakerRole.AI_PROPOSITION
                        AdvanceResult.NextTurn(SpeakerRole.AI_PROPOSITION, StructuredPhase.REBUTTAL)
                    }
                    else -> error("Unexpected speaker in OPENING: $currentSpeaker")
                }
            }
            StructuredPhase.REBUTTAL -> {
                when (currentSpeaker) {
                    SpeakerRole.AI_PROPOSITION -> {
                        currentSpeaker = SpeakerRole.AI_OPPOSITION
                        AdvanceResult.NextTurn(SpeakerRole.AI_OPPOSITION, phase)
                    }
                    SpeakerRole.AI_OPPOSITION -> {
                        currentPhase = StructuredPhase.CLOSING
                        currentSpeaker = SpeakerRole.AI_PROPOSITION
                        AdvanceResult.NextTurn(SpeakerRole.AI_PROPOSITION, StructuredPhase.CLOSING)
                    }
                    else -> error("Unexpected speaker in REBUTTAL: $currentSpeaker")
                }
            }
            StructuredPhase.CLOSING -> {
                when (currentSpeaker) {
                    SpeakerRole.AI_PROPOSITION -> {
                        currentSpeaker = SpeakerRole.AI_OPPOSITION
                        AdvanceResult.NextTurn(SpeakerRole.AI_OPPOSITION, phase)
                    }
                    SpeakerRole.AI_OPPOSITION -> {
                        AdvanceResult.Completed
                    }
                    else -> error("Unexpected speaker in CLOSING: $currentSpeaker")
                }
            }
        }
        return nextTurn
    }

    private fun advanceFreeFlow(): AdvanceResult {
        currentSpeaker = when (currentSpeaker) {
            SpeakerRole.AI_PROPOSITION -> SpeakerRole.AI_OPPOSITION
            SpeakerRole.AI_OPPOSITION -> SpeakerRole.AI_PROPOSITION
            SpeakerRole.USER -> SpeakerRole.AI_OPPOSITION
            else -> SpeakerRole.AI_OPPOSITION
        }
        return AdvanceResult.NextTurn(currentSpeaker, null)
    }

    sealed interface AdvanceResult {
        data class NextTurn(val speaker: SpeakerRole, val phase: StructuredPhase?) : AdvanceResult
        data object Completed : AdvanceResult
    }
}
