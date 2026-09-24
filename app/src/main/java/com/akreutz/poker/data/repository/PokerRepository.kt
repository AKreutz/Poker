package com.akreutz.poker.data.repository

import com.akreutz.poker.data.local.dao.PlayerDao
import com.akreutz.poker.data.local.dao.PurgedIdDao
import com.akreutz.poker.data.local.dao.SessionDao
import com.akreutz.poker.data.local.dao.SessionEntryDao
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.PurgedIdEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.data.sync.LocalChangeTracker
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PokerRepository(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val sessionEntryDao: SessionEntryDao,
    private val purgedIdDao: PurgedIdDao,
    private val localChangeTracker: LocalChangeTracker,
) {
    fun observeActivePlayers(): Flow<List<PlayerEntity>> = playerDao.observeActivePlayersBySessionsPlayed()

    fun observeActivePlayersWithSessionCount(): Flow<List<PlayerWithSessionCount>> =
        playerDao.observeActivePlayersWithSessionCount()

    suspend fun getPlayerById(id: String): PlayerEntity? = playerDao.getById(id)

    fun observeSessionsWithEntries(): Flow<List<SessionWithEntries>> =
        sessionDao.observeSessionsWithEntries()
            .map { sessions -> sessions.map { it.withoutDeletedEntries() } }

    /** All sessions regardless of status or soft-delete state, for debugging. */
    fun observeAllSessions(): Flow<List<SessionEntity>> =
        sessionDao.observeAllSessions()

    suspend fun getSessionsWithEntries(): List<SessionWithEntries> =
        sessionDao.observeSessionsWithEntries().first()

    fun observeOpenSession(): Flow<SessionWithEntries?> =
        sessionDao.observeSessionWithEntriesByStatus(SessionStatus.OPEN)
            .map { it?.withoutDeletedEntries() }

    fun observeAllTimePlayerTotals(): Flow<List<PlayerTotals>> =
        sessionEntryDao.observeAllTimePlayerTotals()

    suspend fun getOrCreatePlayer(name: String): PlayerEntity {
        playerDao.findByName(name)?.let { return it }
        val now = Instant.now()
        val player = PlayerEntity(name = name, createdAt = now, updatedAt = now)
        playerDao.insert(player)
        localChangeTracker.markDirty()
        return player
    }

    suspend fun createSession(date: LocalDate): SessionEntity {
        val now = Instant.now()
        val session = SessionEntity(date = date, status = SessionStatus.OPEN, createdAt = now, updatedAt = now)
        sessionDao.insert(session)
        localChangeTracker.markDirty()
        return session
    }

    suspend fun updateHandsWon(entryId: String, handsWon: Int?) {
        sessionEntryDao.updateHandsWon(entryId, handsWon, Instant.now())
        localChangeTracker.markDirty()
    }

    suspend fun clearHandsWonForSession(sessionId: String) {
        sessionEntryDao.clearHandsWonForSession(sessionId, Instant.now())
        localChangeTracker.markDirty()
    }

    suspend fun concludeSession(session: SessionEntity, concludedAt: Instant = Instant.now()) {
        sessionDao.update(session.copy(status = SessionStatus.CONCLUDED, updatedAt = concludedAt))
        localChangeTracker.markDirty()
    }

    suspend fun cancelSession(session: SessionEntity) {
        sessionDao.update(session.copy(isDeleted = true, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    suspend fun deleteSession(session: SessionEntity) {
        sessionDao.update(session.copy(isDeleted = true, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    /**
     * Permanently removes every soft-deleted session (and, via the FK cascade, its entries)
     * from the local database. Every removed id - sessions and their entries alike - is
     * recorded in the purge log first, so the hard-delete survives sync instead of the rows
     * reappearing from a remote snapshot or another device that still has them soft-deleted.
     * See [com.akreutz.poker.data.sync.SyncManager] for how the purge log is used during merge.
     */
    suspend fun pruneDeletedSessions() {
        val sessionsToPurge = sessionDao.getSoftDeleted()
        if (sessionsToPurge.isEmpty()) return

        val sessionIds = sessionsToPurge.map { it.id }
        val entriesToPurge = sessionEntryDao.getForSessions(sessionIds)

        val now = Instant.now()
        val purgedIds = (sessionIds + entriesToPurge.map { it.id }).map { PurgedIdEntity(id = it, purgedAt = now) }
        purgedIdDao.insertAll(purgedIds)

        sessionDao.deleteSoftDeleted()
        localChangeTracker.markDirty()
    }

    suspend fun addEntry(
        sessionId: String,
        playerId: String,
        buyInCents: Long,
        cashOutCents: Long,
    ) {
        val now = Instant.now()
        sessionEntryDao.insert(
            SessionEntryEntity(
                sessionId = sessionId,
                playerId = playerId,
                buyInCents = buyInCents,
                cashOutCents = cashOutCents,
                createdAt = now,
                updatedAt = now,
            )
        )
        localChangeTracker.markDirty()
    }

    suspend fun updateEntry(entry: SessionEntryEntity) {
        sessionEntryDao.update(entry.copy(updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    suspend fun softDeleteEntry(entry: SessionEntryEntity) {
        sessionEntryDao.update(entry.copy(isDeleted = true, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        playerDao.update(player.copy(isDeleted = true, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }
}

/**
 * Room's [androidx.room.Relation] used to load [SessionWithEntries.entries] has no way to
 * express a WHERE clause, so soft-deleted entries come back embedded alongside live ones.
 * Every read path filters them out here instead.
 */
private fun SessionWithEntries.withoutDeletedEntries(): SessionWithEntries =
    copy(entries = entries.filterNot { it.entry.isDeleted })
