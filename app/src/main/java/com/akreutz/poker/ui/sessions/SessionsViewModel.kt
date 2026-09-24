package com.akreutz.poker.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PokerRecords
import com.akreutz.poker.data.model.SessionResult
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.model.computePokerRecords
import com.akreutz.poker.data.model.computeSessionResult
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionsViewModel(private val repository: PokerRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionWithEntries>> = repository.observeSessionsWithEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val sessionResultsById: StateFlow<Map<String, SessionResult>> = repository.observeSessionsWithEntries()
        .map { sessions -> computeSessionResultsById(sessions) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyMap(),
        )

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun removePlayerFromSession(entry: SessionEntryEntity) {
        viewModelScope.launch {
            repository.softDeleteEntry(entry)
        }
    }
}

private fun computeSessionResultsById(sessions: List<SessionWithEntries>): Map<String, SessionResult> {
    val chronological = sessions.sortedBy { it.session.date }
    var recordsBefore: PokerRecords? = null
    val resultsById = mutableMapOf<String, SessionResult>()
    chronological.forEachIndexed { index, sessionWithEntries ->
        val sessionsUpToHere = chronological.subList(0, index + 1)
        val recordsAfter = computePokerRecords(sessionsUpToHere)
        resultsById[sessionWithEntries.session.id] =
            computeSessionResult(sessionWithEntries, recordsBefore, recordsAfter)
        recordsBefore = recordsAfter
    }
    return resultsById
}
