package com.akreutz.poker.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether local data has changed since the last successful push to the remote.
 * [PokerRepository] marks this dirty on every write; [SyncManager] clears it once a sync
 * has pushed the current state. In-memory only - a process restart triggers a fresh sync
 * anyway, so losing this flag on death is harmless.
 */
class LocalChangeTracker {
    private val _hasUnsyncedChanges = MutableStateFlow(false)
    val hasUnsyncedChanges: StateFlow<Boolean> = _hasUnsyncedChanges.asStateFlow()

    fun markDirty() {
        _hasUnsyncedChanges.value = true
    }

    fun markSynced() {
        _hasUnsyncedChanges.value = false
    }
}
