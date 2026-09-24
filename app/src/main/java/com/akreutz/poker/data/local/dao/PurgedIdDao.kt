package com.akreutz.poker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.akreutz.poker.data.local.entity.PurgedIdEntity

@Dao
interface PurgedIdDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(ids: List<PurgedIdEntity>)

    /** Insert-or-replace used when merging a remote snapshot into the local database. */
    @Upsert
    suspend fun upsertAll(ids: List<PurgedIdEntity>)

    @Query("SELECT * FROM purged_ids")
    suspend fun getAll(): List<PurgedIdEntity>
}
