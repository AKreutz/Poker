package com.akreutz.poker.data.watch

import com.akreutz.poker.data.model.SessionWithEntries

/**
 * Wire format between the phone and the Connect IQ watch app. Messages are plain maps/lists of
 * strings, numbers and booleans, which is all the Connect IQ message channel can carry. Keys are
 * kept short because messages are size-limited on older watches.
 *
 * Phone -> watch (full state, sent on every change and on request):
 * ```
 * {"t": "state", "rec": Boolean, "hp": Int, "p": [{"i": shortId, "n": name, "w": handsWon}, ...]}
 * {"t": "idle"}   // no open session
 * ```
 * Watch -> phone:
 * ```
 * {"t": "adj", "i": shortId, "d": 1 | -1, "m": messageId}
 * {"t": "sync"}   // asks for the current state, e.g. when the watch app starts
 * ```
 * The watch should show the counts from the latest state message rather than count optimistically,
 * and send "sync" on start. Message ids only need to be unique within one run of the watch app.
 */
object WatchProtocol {
    const val KEY_TYPE = "t"
    const val KEY_RECORDING = "rec"
    const val KEY_HANDS_PLAYED = "hp"
    const val KEY_PLAYERS = "p"
    const val KEY_ID = "i"
    const val KEY_NAME = "n"
    const val KEY_HANDS_WON = "w"
    const val KEY_DELTA = "d"
    const val KEY_MESSAGE_ID = "m"

    const val TYPE_STATE = "state"
    const val TYPE_IDLE = "idle"
    const val TYPE_ADJUST = "adj"
    const val TYPE_SYNC = "sync"

    private const val SHORT_ID_LENGTH = 8
    private const val MAX_NAME_LENGTH = 12

    /** Entry ids are 36-character UUIDs; the first few characters identify one within a session. */
    fun shortId(entryId: String): String = entryId.take(SHORT_ID_LENGTH)

    fun encodeState(session: SessionWithEntries?, recordHandsWon: Boolean): Map<String, Any> {
        if (session == null) return mapOf(KEY_TYPE to TYPE_IDLE)
        return mapOf(
            KEY_TYPE to TYPE_STATE,
            KEY_RECORDING to recordHandsWon,
            KEY_HANDS_PLAYED to (session.handsPlayed ?: 0),
            KEY_PLAYERS to session.entries.map {
                mapOf(
                    KEY_ID to shortId(it.entry.id),
                    KEY_NAME to it.player.name.take(MAX_NAME_LENGTH),
                    KEY_HANDS_WON to (it.entry.handsWon ?: 0),
                )
            },
        )
    }

    /** Returns null for anything that isn't a well-formed command, so a bad message is ignored. */
    fun decodeCommand(message: Any?): WatchCommand? {
        val map = message as? Map<*, *> ?: return null
        return when (map[KEY_TYPE]) {
            TYPE_SYNC -> WatchCommand.RequestState
            TYPE_ADJUST -> {
                val shortId = (map[KEY_ID] as? String)?.takeIf { it.isNotEmpty() } ?: return null
                val delta = (map[KEY_DELTA] as? Number)?.toInt()?.takeIf { it == 1 || it == -1 } ?: return null
                WatchCommand.Adjust(shortId, delta, (map[KEY_MESSAGE_ID] as? Number)?.toLong())
            }
            else -> null
        }
    }
}

sealed interface WatchCommand {
    data object RequestState : WatchCommand

    /** [messageId] lets the phone drop a retransmitted command instead of counting a hand twice. */
    data class Adjust(val shortId: String, val delta: Int, val messageId: Long?) : WatchCommand
}
