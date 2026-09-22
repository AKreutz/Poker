package com.akreutz.poker.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionsViewModel(repository: PokerRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionWithEntries>> = repository.observeSessionsWithEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    class Factory(private val repository: PokerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionsViewModel(repository) as T
        }
    }
}
