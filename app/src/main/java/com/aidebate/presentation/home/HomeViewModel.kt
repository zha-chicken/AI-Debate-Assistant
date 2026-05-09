package com.aidebate.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aidebate.domain.model.DebateMode
import com.aidebate.domain.model.DebateResult
import com.aidebate.domain.model.DebateSessionSummary
import com.aidebate.domain.model.SpeakerRole
import com.aidebate.domain.repository.DebateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeStatsUiState(
    val debateCount: Int = 0,
    val winRatePercent: Int = 0,
    val winStreak: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    debateRepository: DebateRepository,
) : ViewModel() {

    val stats: StateFlow<HomeStatsUiState> = combine(
        debateRepository.getAllSessions(),
        debateRepository.getAllResults(),
    ) { sessions, results ->
        calculateStats(sessions, results)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeStatsUiState(),
    )

    private fun calculateStats(
        sessions: List<DebateSessionSummary>,
        results: List<DebateResult>,
    ): HomeStatsUiState {
        val resultsBySessionId = results.associateBy { it.sessionId }
        val judgedUserDebates = sessions
            .filter { it.mode == DebateMode.USER_VS_AI }
            .mapNotNull { session ->
                val result = resultsBySessionId[session.id] ?: return@mapNotNull null
                val userSide = session.userSide ?: return@mapNotNull null
                JudgedDebate(
                    createdAt = session.createdAt,
                    userWon = result.winner == SpeakerRole.USER || result.winner == userSide,
                )
            }

        val userWins = judgedUserDebates.count { it.userWon }
        val winRate = if (judgedUserDebates.isEmpty()) {
            0
        } else {
            ((userWins * 100f) / judgedUserDebates.size).toInt()
        }
        val streak = judgedUserDebates
            .sortedByDescending { it.createdAt }
            .takeWhile { it.userWon }
            .size

        return HomeStatsUiState(
            debateCount = sessions.size,
            winRatePercent = winRate,
            winStreak = streak,
        )
    }

    private data class JudgedDebate(
        val createdAt: Long,
        val userWon: Boolean,
    )
}
