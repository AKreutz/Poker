package com.akreutz.poker.ui.currentsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.SessionResult
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.model.computePokerRecords
import com.akreutz.poker.data.model.computeSessionResult
import com.akreutz.poker.data.repository.PokerRepository
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurrentSessionViewModel(private val repository: PokerRepository) : ViewModel() {
    val openSession: StateFlow<SessionWithEntries?> = repository.observeOpenSession()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val players: StateFlow<List<PlayerWithSessionCount>> = repository.observeActivePlayersWithSessionCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _showPlayerSelection = MutableStateFlow(false)
    val showPlayerSelection: StateFlow<Boolean> = _showPlayerSelection.asStateFlow()

    private val _showAddPlayer = MutableStateFlow(false)
    val showAddPlayer: StateFlow<Boolean> = _showAddPlayer.asStateFlow()

    private val _showConcludeDialog = MutableStateFlow(false)
    val showConcludeDialog: StateFlow<Boolean> = _showConcludeDialog.asStateFlow()

    private val _sessionResult = MutableStateFlow<SessionResult?>(null)
    val sessionResult: StateFlow<SessionResult?> = _sessionResult.asStateFlow()

    private val _recordHandsPlayed = MutableStateFlow(false)
    val recordHandsPlayed: StateFlow<Boolean> = _recordHandsPlayed.asStateFlow()

    fun startSession() {
        _showPlayerSelection.value = true
    }

    fun confirmPlayerSelection(selectedPlayerIds: Set<String>, newPlayerNames: Set<String>, buyInCents: Long) {
        viewModelScope.launch {
            val newPlayerIds = newPlayerNames.map { repository.getOrCreatePlayer(it).id }
            val session = repository.createSession(LocalDate.now())
            (selectedPlayerIds + newPlayerIds).forEach { playerId ->
                repository.addEntry(
                    sessionId = session.id,
                    playerId = playerId,
                    buyInCents = buyInCents,
                    cashOutCents = 0,
                )
            }
            _showPlayerSelection.value = false
        }
    }

    fun dismissPlayerSelection() {
        _showPlayerSelection.value = false
    }

    fun addPlayerToSession() {
        _showAddPlayer.value = true
    }

    fun confirmAddPlayer(selectedPlayerIds: Set<String>, newPlayerNames: Set<String>, buyInCents: Long) {
        val session = openSession.value?.session ?: return
        viewModelScope.launch {
            val newPlayerIds = newPlayerNames.map { repository.getOrCreatePlayer(it).id }
            (selectedPlayerIds + newPlayerIds).forEach { playerId ->
                repository.addEntry(
                    sessionId = session.id,
                    playerId = playerId,
                    buyInCents = buyInCents,
                    cashOutCents = 0,
                )
            }
            _showAddPlayer.value = false
        }
    }

    fun dismissAddPlayer() {
        _showAddPlayer.value = false
    }

    fun increaseBuyIn(entry: SessionEntryEntity, additionalCents: Long) {
        viewModelScope.launch {
            repository.updateEntry(entry.copy(buyInCents = entry.buyInCents + additionalCents))
        }
    }

    fun adjustHandsWon(entry: SessionEntryEntity, delta: Int) {
        val sessionWithEntries = openSession.value ?: return
        val previousTotal = sessionWithEntries.handsPlayed ?: 0
        val previousCount = entry.handsWon ?: 0
        val newCount = previousCount + delta
        if (newCount < 0) return
        val newTotal = previousTotal + delta
        if (previousTotal == 0 && newTotal > 0) {
            // First hand recorded this session: auto-opt-in.
            _recordHandsPlayed.value = true
        } else if (newTotal == 0) {
            // Back to zero: nothing to record, so opting in is meaningless.
            _recordHandsPlayed.value = false
        }
        viewModelScope.launch {
            repository.updateHandsWon(entry.id, newCount)
        }
    }

    fun setRecordHandsPlayed(record: Boolean) {
        val handsPlayed = openSession.value?.handsPlayed ?: 0
        if (record && handsPlayed <= 0) return
        _recordHandsPlayed.value = record
    }

    fun concludeSession() {
        _showConcludeDialog.value = true
    }

    fun dismissConcludeDialog() {
        _showConcludeDialog.value = false
    }

    fun confirmConclude(cashOutsByEntryId: Map<String, Long>) {
        val sessionWithEntries = openSession.value ?: return
        viewModelScope.launch {
            val sessionsBefore = repository.getSessionsWithEntries()
            val recordsBefore = if (sessionsBefore.isEmpty()) null else computePokerRecords(sessionsBefore)

            val shouldRecordHandsPlayed = _recordHandsPlayed.value

            sessionWithEntries.entries.forEach { entryWithPlayer ->
                val cashOutCents = cashOutsByEntryId[entryWithPlayer.entry.id] ?: return@forEach
                repository.updateEntry(entryWithPlayer.entry.copy(cashOutCents = cashOutCents))
            }
            val concludedAt = Instant.now()
            repository.concludeSession(sessionWithEntries.session, concludedAt)
            if (!shouldRecordHandsPlayed) {
                repository.clearHandsWonForSession(sessionWithEntries.session.id)
            }
            _showConcludeDialog.value = false
            _recordHandsPlayed.value = false

            val updatedEntries = sessionWithEntries.entries.map { entryWithPlayer ->
                val cashOutCents = cashOutsByEntryId[entryWithPlayer.entry.id] ?: entryWithPlayer.entry.cashOutCents
                val handsWon = entryWithPlayer.entry.handsWon.takeIf { shouldRecordHandsPlayed }
                entryWithPlayer.copy(
                    entry = entryWithPlayer.entry.copy(cashOutCents = cashOutCents, handsWon = handsWon),
                )
            }
            val concludedSession = sessionWithEntries.copy(
                session = sessionWithEntries.session.copy(updatedAt = concludedAt),
                entries = updatedEntries,
            )

            val sessionsAfter = repository.getSessionsWithEntries()
            val recordsAfter = computePokerRecords(sessionsAfter)

            _sessionResult.value = computeSessionResult(concludedSession, recordsBefore, recordsAfter)
        }
    }

    fun dismissSessionResult() {
        _sessionResult.value = null
    }

    fun cancelSession() {
        val session = openSession.value?.session ?: return
        viewModelScope.launch {
            repository.cancelSession(session)
        }
    }
}
