package com.akreutz.poker.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionsDebugViewModel(
    private val repository: PokerRepository,
    private val onPruned: () -> Unit,
) : ViewModel() {
    val sessions: StateFlow<List<SessionEntity>> = repository.observeAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Hard-deletes every soft-deleted session and triggers a sync push right after, so the
     * purge overwrites the shared remote snapshot instead of only living on this device (see
     * [PokerRepository.pruneDeletedSessions]). A device that hasn't synced since will still
     * resurrect its own copies on its next sync - the purge log is what stops them from
     * sticking once that device catches up too.
     */
    fun pruneDeleted() {
        viewModelScope.launch {
            repository.pruneDeletedSessions()
            onPruned()
        }
    }
}
