package com.akreutz.poker.data.repository

import com.akreutz.poker.data.local.dao.PlayerDao
import com.akreutz.poker.data.local.dao.SessionDao
import com.akreutz.poker.data.local.dao.SessionEntryDao
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.SessionWithEntries
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class PokerRepository(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val sessionEntryDao: SessionEntryDao,
) {
    fun observeActivePlayers(): Flow<List<PlayerEntity>> = playerDao.observeActivePlayers()

    fun observeSessionsWithEntries(): Flow<List<SessionWithEntries>> =
        sessionDao.observeSessionsWithEntries()

    fun observeAllTimePlayerTotals(): Flow<List<PlayerTotals>> =
        sessionEntryDao.observeAllTimePlayerTotals()

    suspend fun getOrCreatePlayer(name: String): PlayerEntity {
        playerDao.findByName(name)?.let { return it }
        val now = Instant.now()
        val player = PlayerEntity(name = name, createdAt = now, updatedAt = now)
        playerDao.insert(player)
        return player
    }

    suspend fun createSession(date: LocalDate): SessionEntity {
        val now = Instant.now()
        val session = SessionEntity(date = date, createdAt = now, updatedAt = now)
        sessionDao.insert(session)
        return session
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
    }

    suspend fun updateEntry(entry: SessionEntryEntity) =
        sessionEntryDao.update(entry.copy(updatedAt = Instant.now()))

    suspend fun softDeleteEntry(entry: SessionEntryEntity) =
        sessionEntryDao.update(entry.copy(isDeleted = true, updatedAt = Instant.now()))

    suspend fun isEmpty(): Boolean = sessionDao.count() == 0
}
