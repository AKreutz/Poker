package com.akreutz.poker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akreutz.poker.data.local.dao.PlayerDao
import com.akreutz.poker.data.local.dao.SessionDao
import com.akreutz.poker.data.local.dao.SessionEntryDao
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity

@Database(
    entities = [PlayerEntity::class, SessionEntity::class, SessionEntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionEntryDao(): SessionEntryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poker.db",
                ).build().also { instance = it }
            }
    }
}
