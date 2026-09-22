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

    @Query("SELECT * FROM players WHERE isDeleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeActivePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun findByName(name: String): PlayerEntity?

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getById(id: String): PlayerEntity?

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int
}
