package com.akreutz.poker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.akreutz.poker.data.local.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert
    suspend fun insert(player: PlayerEntity)

    @Insert
    suspend fun insertAll(players: List<PlayerEntity>)

    @Update
    suspend fun update(player: PlayerEntity)

    @Query(
        """
        SELECT p.* FROM players p
        LEFT JOIN (
            SELECT playerId, COUNT(*) AS sessionsPlayed
            FROM session_entries
            WHERE isDeleted = 0
            GROUP BY playerId
        ) e ON e.playerId = p.id
        WHERE p.isDeleted = 0
        ORDER BY COALESCE(e.sessionsPlayed, 0) DESC, p.name COLLATE NOCASE
        """
    )
    fun observeActivePlayersBySessionsPlayed(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun findByName(name: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: String): PlayerEntity?

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int
}
