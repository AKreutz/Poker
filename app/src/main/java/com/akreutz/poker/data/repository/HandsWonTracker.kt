package com.akreutz.poker.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the live hands-won counters of the open session, so the phone UI and the watch change
 * them through the same code path. Adjustments are serialised and re-read the session from the
 * database each time, so rapid taps from both sources can't overwrite one another.
 */
class HandsWonTracker(private val repository: PokerRepository) {
    private val mutex = Mutex()

    private val _recordHandsPlayed = MutableStateFlow(false)
    val recordHandsPlayed: StateFlow<Boolean> = _recordHandsPlayed.asStateFlow()

    /** Returns false if there is no open session, no such entry, or the count would go negative. */
    suspend fun adjust(entryId: String, delta: Int): Boolean = mutex.withLock {
        val session = repository.observeOpenSession().first() ?: return false
        val entry = session.entries.firstOrNull { it.entry.id == entryId }?.entry ?: return false
        val previousTotal = session.handsPlayed ?: 0
        val newCount = (entry.handsWon ?: 0) + delta
        if (newCount < 0) return false
        val newTotal = previousTotal + delta
        if (previousTotal == 0 && newTotal > 0) {
            // First hand recorded this session: auto-opt-in.
            _recordHandsPlayed.value = true
        } else if (newTotal == 0) {
            // Back to zero: nothing to record, so opting in is meaningless.
            _recordHandsPlayed.value = false
        }
        repository.updateHandsWon(entry.id, newCount)
        true
    }

    suspend fun setRecordHandsPlayed(record: Boolean) = mutex.withLock {
        val handsPlayed = repository.observeOpenSession().first()?.handsPlayed ?: 0
        if (record && handsPlayed <= 0) return@withLock
        _recordHandsPlayed.value = record
    }

    /** Called once a session concludes or is cancelled, so the next session starts opted out. */
    fun reset() {
        _recordHandsPlayed.value = false
    }
}
