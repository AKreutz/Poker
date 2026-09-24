package com.akreutz.poker.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

/** Adds the nullable `handsPlayed` column to `sessions`; existing rows get NULL (unknown). */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN handsPlayed INTEGER")
    }
}

/** Adds the `purged_ids` table used to make hard-deletes stick across sync. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS purged_ids (id TEXT NOT NULL PRIMARY KEY, purgedAt INTEGER NOT NULL)"
        )
    }
}
