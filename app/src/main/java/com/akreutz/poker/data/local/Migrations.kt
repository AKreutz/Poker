package com.akreutz.poker.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

/** Adds the nullable `handsPlayed` column to `sessions`; existing rows get NULL (unknown). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN handsPlayed INTEGER")
    }
}
