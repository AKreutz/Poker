package com.akreutz.poker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PlayerTotals
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEntryDao {
    @Insert
    suspend fun insert(entry: SessionEntryEntity)

    @Insert
    suspend fun insertAll(entries: List<SessionEntryEntity>)

    @Update
    suspend fun update(entry: SessionEntryEntity)

    /** Insert-or-replace used when merging a remote snapshot into the local database. */
    @Upsert
    suspend fun upsertAll(entries: List<SessionEntryEntity>)

    /** All rows including soft-deleted ones, for building a full sync snapshot. */
    @Query("SELECT * FROM session_entries")
    suspend fun getAll(): List<SessionEntryEntity>

    @Query("SELECT * FROM session_entries WHERE sessionId = :sessionId AND isDeleted = 0")
    fun observeEntriesForSession(sessionId: String): Flow<List<SessionEntryEntity>>

    @Query("SELECT * FROM session_entries WHERE playerId = :playerId AND isDeleted = 0")
    fun observeEntriesForPlayer(playerId: String): Flow<List<SessionEntryEntity>>

    @Query(
        """
        SELECT p.id AS playerId, p.name AS playerName,
               COUNT(*) AS sessionsPlayed,
               SUM(e.buyInCents) AS totalBuyInCents,
               SUM(e.cashOutCents) AS totalCashOutCents,
               SUM(e.cashOutCents - e.buyInCents) AS totalDeltaCents
        FROM session_entries e
        JOIN players p ON p.id = e.playerId
        JOIN sessions s ON s.id = e.sessionId
        WHERE e.isDeleted = 0 AND p.isDeleted = 0 AND s.isDeleted = 0 AND s.status = 'CONCLUDED'
        GROUP BY p.id
        ORDER BY totalDeltaCents DESC
        """
    )
    fun observeAllTimePlayerTotals(): Flow<List<PlayerTotals>>
}
