package com.akreutz.poker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.SinglePlayerRecords
import com.akreutz.poker.data.model.chronologicalDeltasByPlayerId
import com.akreutz.poker.data.model.computeSinglePlayerRecords
import com.akreutz.poker.data.model.standardDeviation
import com.akreutz.poker.data.repository.PokerRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerSessionResult(
    val sessionDate: LocalDate,
    val buyInCents: Long,
    val cashOutCents: Long,
) {
    val deltaCents: Long get() = cashOutCents - buyInCents
}

data class PlayerDetailUiState(
    val player: PlayerEntity?,
    val totals: PlayerTotals?,
    val sessionResults: List<PlayerSessionResult>,
    val records: SinglePlayerRecords?,
    val standardDeviationCents: Double?,
)

class PlayerDetailViewModel(
    private val repository: PokerRepository,
    private val playerId: String,
) : ViewModel() {
    private val player = MutableStateFlow<PlayerEntity?>(null)

    init {
        viewModelScope.launch {
            player.value = repository.getPlayerById(playerId)
        }
    }

    val uiState: StateFlow<PlayerDetailUiState> = combine(
        player,
        repository.observeAllTimePlayerTotals(),
        repository.observeSessionsWithEntries(),
    ) { player, totals, sessions ->
        val sessionResultsChronological = sessions
            .filter { it.session.status == SessionStatus.CONCLUDED }
            .sortedBy { it.session.date }
            .mapNotNull { sessionWithEntries ->
                val entry = sessionWithEntries.entries.firstOrNull { it.entry.playerId == playerId }
                entry?.let {
                    PlayerSessionResult(
                        sessionDate = sessionWithEntries.session.date,
                        buyInCents = it.entry.buyInCents,
                        cashOutCents = it.entry.cashOutCents,
                    )
                }
            }
        val deltasChronological = chronologicalDeltasByPlayerId(sessions)[playerId].orEmpty()

        PlayerDetailUiState(
            player = player,
            totals = totals.firstOrNull { it.playerId == playerId },
            sessionResults = sessionResultsChronological.sortedByDescending { it.sessionDate },
            records = computeSinglePlayerRecords(
                playerName = player?.name.orEmpty(),
                deltasChronological = deltasChronological,
            ),
            standardDeviationCents = if (deltasChronological.size >= 2) {
                deltasChronological.map { it.deltaCents }.standardDeviation()
            } else {
                null
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlayerDetailUiState(
            player = null,
            totals = null,
            sessionResults = emptyList(),
            records = null,
            standardDeviationCents = null,
        ),
    )
}
