package com.akreutz.poker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val MIN_SESSIONS_FOR_BALANCE = 5

class HomeViewModel(repository: PokerRepository) : ViewModel() {
    val mostRecentSession: StateFlow<SessionWithEntries?> = repository.observeSessionsWithEntries()
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val playerBalances: StateFlow<List<PlayerTotals>> = repository.observeAllTimePlayerTotals()
        .map { totals -> totals.filter { it.sessionsPlayed > MIN_SESSIONS_FOR_BALANCE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    class Factory(private val repository: PokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
