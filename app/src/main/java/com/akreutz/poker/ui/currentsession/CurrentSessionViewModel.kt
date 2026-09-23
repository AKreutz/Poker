package com.akreutz.poker.ui.currentsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.repository.PokerRepository
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

    private val _showConcludeDialog = MutableStateFlow(false)
    val showConcludeDialog: StateFlow<Boolean> = _showConcludeDialog.asStateFlow()

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

    fun increaseBuyIn(entry: SessionEntryEntity, additionalCents: Long) {
        viewModelScope.launch {
            repository.updateEntry(entry.copy(buyInCents = entry.buyInCents + additionalCents))
        }
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
            sessionWithEntries.entries.forEach { entryWithPlayer ->
                val cashOutCents = cashOutsByEntryId[entryWithPlayer.entry.id] ?: return@forEach
                repository.updateEntry(entryWithPlayer.entry.copy(cashOutCents = cashOutCents))
            }
            repository.concludeSession(sessionWithEntries.session)
            _showConcludeDialog.value = false
        }
    }

    fun cancelSession() {
        val session = openSession.value?.session ?: return
        viewModelScope.launch {
            repository.cancelSession(session)
        }
    }

    class Factory(private val repository: PokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CurrentSessionViewModel(repository) as T
        }
    }
}
