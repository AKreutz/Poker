package com.akreutz.poker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.model.SessionWithEntries
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Insert
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE isDeleted = 0 ORDER BY date DESC")
    fun observeActiveSessions(): Flow<List<SessionEntity>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE isDeleted = 0 AND status = 'CONCLUDED' ORDER BY date DESC")
    fun observeSessionsWithEntries(): Flow<List<SessionWithEntries>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE isDeleted = 0 AND status = :status LIMIT 1")
    fun observeSessionWithEntriesByStatus(status: SessionStatus = SessionStatus.OPEN): Flow<SessionWithEntries?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getById(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE isDeleted = 0 AND status = :status LIMIT 1")
    suspend fun findByStatus(status: SessionStatus = SessionStatus.OPEN): SessionEntity?

    @Query("SELECT * FROM sessions WHERE date = :date AND isDeleted = 0 LIMIT 1")
    suspend fun findByDate(date: LocalDate): SessionEntity?

    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun count(): Int
}
