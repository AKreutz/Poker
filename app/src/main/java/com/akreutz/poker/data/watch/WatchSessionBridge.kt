package com.akreutz.poker.data.watch

import com.akreutz.poker.data.repository.HandsWonTracker
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/** Connects the open session to a watch, independent of which SDK carries the messages. */
class WatchSessionBridge(
    private val repository: PokerRepository,
    private val handsWonTracker: HandsWonTracker,
) {
    /** The full state message to show on the watch, emitted whenever it changes. */
    val state: Flow<Map<String, Any>> = combine(
        repository.observeOpenSession(),
        handsWonTracker.recordHandsPlayed,
        WatchProtocol::encodeState,
    ).distinctUntilChanged()

    private val seenMessageIds = ArrayDeque<Long>()

    /**
     * Applies [command]. Returns a message to send straight back, or null when the effect will
     * show up on [state] anyway (an adjustment changes the database, which re-emits the state).
     */
    suspend fun handle(command: WatchCommand): Map<String, Any>? = when (command) {
        WatchCommand.RequestState -> {
            // A watch app (re)start asks for state first and may reuse message ids from before.
            seenMessageIds.clear()
            currentState()
        }
        is WatchCommand.Adjust -> {
            if (isNewMessage(command.messageId)) adjust(command)
            null
        }
    }

    private suspend fun currentState(): Map<String, Any> =
        WatchProtocol.encodeState(repository.observeOpenSession().first(), handsWonTracker.recordHandsPlayed.value)

    private suspend fun adjust(command: WatchCommand.Adjust) {
        val session = repository.observeOpenSession().first() ?: return
        val matches = session.entries.filter { it.entry.id.startsWith(command.shortId) }
        // An ambiguous prefix is dropped rather than guessed at.
        val entry = matches.singleOrNull()?.entry ?: return
        handsWonTracker.adjust(entry.id, command.delta)
    }

    /** Remembers the last few message ids; a null id (watch didn't send one) is always applied. */
    private fun isNewMessage(messageId: Long?): Boolean {
        if (messageId == null) return true
        if (messageId in seenMessageIds) return false
        seenMessageIds.addLast(messageId)
        if (seenMessageIds.size > MAX_REMEMBERED_MESSAGES) seenMessageIds.removeFirst()
        return true
    }

    private companion object {
        const val MAX_REMEMBERED_MESSAGES = 64
    }
}
