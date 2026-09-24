package com.akreutz.poker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.PokerRecords
import com.akreutz.poker.data.model.computePokerRecords
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repository: PokerRepository) : ViewModel() {
    val playerBalances: StateFlow<List<PlayerTotals>> = repository.observeAllTimePlayerTotals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val records: StateFlow<PokerRecords?> = repository.observeSessionsWithEntries()
        .map { sessions -> if (sessions.isEmpty()) null else computePokerRecords(sessions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}
