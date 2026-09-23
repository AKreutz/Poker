package com.akreutz.poker.ui.graphs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.chronologicalDeltasByPlayerId
import com.akreutz.poker.data.repository.PokerRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * One point in a player's balance history: their running balance as of a session date, plus
 * that date's index into the shared chronological list of all axis dates
 * ([GraphsUiState.axisDates], which is [GraphsUiState.sessionDates] with a leading fictional
 * "everyone starts at 0" point). Dates are treated as ordinal labels, not a linear time axis:
 * every session - regardless of the calendar gap before it - advances the x-axis by exactly one step.
 */
data class PlayerBalancePoint(
    val sessionDate: LocalDate?,
    val dateIndex: Int,
    val cumulativeBalanceCents: Long,
)

/**
 * One player's cumulative balance across every shared session date, in chronological order,
 * starting from the fictional zero-balance point before the first ever session. Dates before the
 * player's first session are treated as a balance of 0, and dates the player didn't play carry
 * forward their most recently known balance.
 */
data class PlayerBalanceSeries(
    val playerId: String,
    val playerName: String,
    val points: List<PlayerBalancePoint>,
)

/** All chart-relevant data derived from concluded sessions, sharing one date-index axis. */
data class GraphsUiState(
    val sessionDates: List<LocalDate>,
    val balanceSeriesByPlayerId: Map<String, PlayerBalanceSeries>,
) {
    /** [sessionDates] with a leading `null` representing the fictional "everyone starts at 0" point. */
    val axisDates: List<LocalDate?> = listOf(null) + sessionDates
}

class GraphsViewModel(repository: PokerRepository) : ViewModel() {
    val players: StateFlow<List<PlayerWithSessionCount>> = repository.observeActivePlayersWithSessionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<GraphsUiState> = combine(
        repository.observeActivePlayersWithSessionCount(),
        repository.observeSessionsWithEntries(),
    ) { playersWithCount, sessions ->
        val deltasByPlayerId = chronologicalDeltasByPlayerId(sessions)
        val sessionDates = deltasByPlayerId.values
            .flatten()
            .map { it.sessionDate }
            .distinct()
            .sorted()
        val dateIndexByDate = sessionDates.withIndex().associate { (index, date) -> date to index }

        val balanceSeriesByPlayerId = playersWithCount.mapNotNull { playerWithCount ->
            val deltas = deltasByPlayerId[playerWithCount.player.id] ?: return@mapNotNull null
            var runningBalance = 0L
            val balanceByDate = mutableMapOf<LocalDate, Long>()
            for (delta in deltas) {
                runningBalance += delta.deltaCents
                balanceByDate[delta.sessionDate] = runningBalance
            }

            var lastKnownBalance = 0L
            val fictionalStartPoint = PlayerBalancePoint(sessionDate = null, dateIndex = 0, cumulativeBalanceCents = 0L)
            val points = listOf(fictionalStartPoint) + sessionDates.mapIndexed { index, date ->
                lastKnownBalance = balanceByDate[date] ?: lastKnownBalance
                PlayerBalancePoint(
                    sessionDate = date,
                    dateIndex = index + 1,
                    cumulativeBalanceCents = lastKnownBalance,
                )
            }
            playerWithCount.player.id to PlayerBalanceSeries(
                playerId = playerWithCount.player.id,
                playerName = playerWithCount.player.name,
                points = points,
            )
        }.toMap()

        GraphsUiState(sessionDates = sessionDates, balanceSeriesByPlayerId = balanceSeriesByPlayerId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GraphsUiState(sessionDates = emptyList(), balanceSeriesByPlayerId = emptyMap()),
    )

    class Factory(private val repository: PokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GraphsViewModel(repository) as T
        }
    }
}
