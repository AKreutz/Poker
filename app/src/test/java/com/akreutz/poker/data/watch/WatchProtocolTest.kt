package com.akreutz.poker.data.watch

import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.model.SessionEntryWithPlayer
import com.akreutz.poker.data.model.SessionWithEntries
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchProtocolTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun entry(playerName: String, handsWon: Int?): SessionEntryWithPlayer {
        val player = PlayerEntity(name = playerName, createdAt = now, updatedAt = now)
        val session = SessionEntity(date = LocalDate.of(2026, 1, 1), status = SessionStatus.OPEN, createdAt = now, updatedAt = now)
        return SessionEntryWithPlayer(
            entry = SessionEntryEntity(
                sessionId = session.id,
                playerId = player.id,
                buyInCents = 1000,
                cashOutCents = 0,
                createdAt = now,
                updatedAt = now,
                handsWon = handsWon,
            ),
            player = player,
        )
    }

    private fun session(vararg entries: SessionEntryWithPlayer) = SessionWithEntries(
        session = SessionEntity(date = LocalDate.of(2026, 1, 1), status = SessionStatus.OPEN, createdAt = now, updatedAt = now),
        entries = entries.toList(),
    )

    @Test
    fun encodeState_withoutSession_isIdle() {
        assertEquals(mapOf("t" to "idle"), WatchProtocol.encodeState(null, recordHandsWon = false))
    }

    @Test
    fun encodeState_listsPlayersWithShortIdsAndCounts() {
        val alice = entry("Alice", handsWon = 3)
        val bob = entry("Bob", handsWon = null)

        val state = WatchProtocol.encodeState(session(alice, bob), recordHandsWon = true)

        assertEquals("state", state["t"])
        assertEquals(true, state["rec"])
        assertEquals(3, state["hp"])
        assertEquals(
            listOf(
                mapOf("i" to alice.entry.id.take(8), "n" to "Alice", "w" to 3),
                mapOf("i" to bob.entry.id.take(8), "n" to "Bob", "w" to 0),
            ),
            state["p"],
        )
    }

    @Test
    fun encodeState_truncatesLongNames() {
        val state = WatchProtocol.encodeState(session(entry("Bartholomew Longname", 0)), recordHandsWon = false)

        val players = state["p"] as List<*>
        assertEquals("Bartholomew ", (players.single() as Map<*, *>)["n"])
    }

    @Test
    fun decodeCommand_adjust() {
        val command = WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234", "d" to 1, "m" to 42L))

        assertEquals(WatchCommand.Adjust("abcd1234", 1, 42L), command)
    }

    @Test
    fun decodeCommand_acceptsAnyNumberTypeFromTheWatch() {
        val command = WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234", "d" to -1.0, "m" to 7))

        assertEquals(WatchCommand.Adjust("abcd1234", -1, 7L), command)
    }

    @Test
    fun decodeCommand_adjustWithoutMessageId() {
        val command = WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234", "d" to 1))

        assertEquals(WatchCommand.Adjust("abcd1234", 1, null), command)
    }

    @Test
    fun decodeCommand_sync() {
        assertEquals(WatchCommand.RequestState, WatchProtocol.decodeCommand(mapOf("t" to "sync")))
    }

    @Test
    fun decodeCommand_rejectsMalformedMessages() {
        assertNull(WatchProtocol.decodeCommand("adj"))
        assertNull(WatchProtocol.decodeCommand(null))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "unknown")))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "adj", "d" to 1)))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "", "d" to 1)))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234", "d" to 5)))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234", "d" to 0)))
        assertNull(WatchProtocol.decodeCommand(mapOf("t" to "adj", "i" to "abcd1234")))
    }
}
