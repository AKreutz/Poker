package com.akreutz.poker.data.repository

import com.akreutz.poker.data.local.dao.PlayerDao
import com.akreutz.poker.data.local.dao.SessionDao
import com.akreutz.poker.data.local.dao.SessionEntryDao
import com.akreutz.poker.data.local.entity.PlayerEntity
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

class PokerRepository(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val sessionEntryDao: SessionEntryDao,
    private val localChangeTracker: LocalChangeTracker,
) {
    fun observeActivePlayers(): Flow<List<PlayerEntity>> = playerDao.observeActivePlayersBySessionsPlayed()

    fun observeActivePlayersWithSessionCount(): Flow<List<PlayerWithSessionCount>> =
        playerDao.observeActivePlayersWithSessionCount()

    fun observeSessionsWithEntries(): Flow<List<SessionWithEntries>> =
        sessionDao.observeSessionsWithEntries()

    fun observeOpenSession(): Flow<SessionWithEntries?> =
        sessionDao.observeSessionWithEntriesByStatus(SessionStatus.OPEN)

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

    suspend fun concludeSession(session: SessionEntity) {
        sessionDao.update(session.copy(status = SessionStatus.CONCLUDED, updatedAt = Instant.now()))
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
