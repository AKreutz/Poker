package com.akreutz.poker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerStats(
    val playerWithCount: PlayerWithSessionCount,
    val totals: PlayerTotals?,
)

class StatsViewModel(private val repository: PokerRepository) : ViewModel() {
    val players: StateFlow<List<PlayerStats>> = combine(
        repository.observeActivePlayersWithSessionCount(),
        repository.observeAllTimePlayerTotals(),
    ) { playersWithCount, totals ->
        val totalsByPlayerId = totals.associateBy { it.playerId }
        playersWithCount.map { playerWithCount ->
            PlayerStats(
                playerWithCount = playerWithCount,
                totals = totalsByPlayerId[playerWithCount.player.id],
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

    class Factory(private val repository: PokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}
