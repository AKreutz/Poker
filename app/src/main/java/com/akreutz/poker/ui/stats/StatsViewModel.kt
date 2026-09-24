package com.akreutz.poker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.SinglePlayerRecords
import com.akreutz.poker.data.model.chronologicalDeltasByPlayerId
import com.akreutz.poker.data.model.computeSinglePlayerRecords
import com.akreutz.poker.data.model.standardDeviation
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerStats(
    val playerWithCount: PlayerWithSessionCount,
    val totals: PlayerTotals?,
    val standardDeviationCents: Double?,
    val records: SinglePlayerRecords?,
)

class StatsViewModel(private val repository: PokerRepository) : ViewModel() {
    val players: StateFlow<List<PlayerStats>> = combine(
        repository.observeActivePlayersWithSessionCount(),
        repository.observeAllTimePlayerTotals(),
        repository.observeSessionsWithEntries(),
    ) { playersWithCount, totals, sessions ->
        val totalsByPlayerId = totals.associateBy { it.playerId }
        val deltasByPlayerId = chronologicalDeltasByPlayerId(sessions)
        playersWithCount.map { playerWithCount ->
            val deltas = deltasByPlayerId[playerWithCount.player.id]
            PlayerStats(
                playerWithCount = playerWithCount,
                totals = totalsByPlayerId[playerWithCount.player.id],
                standardDeviationCents = if (deltas != null && deltas.size >= 2) {
                    deltas.map { it.deltaCents }.standardDeviation()
                } else {
                    null
                },
                records = deltas?.let {
                    computeSinglePlayerRecords(playerWithCount.player.name, it)
                },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }
}
