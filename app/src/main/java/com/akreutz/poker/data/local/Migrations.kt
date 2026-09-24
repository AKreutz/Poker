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

/**
 * Moves hand tracking from a single `sessions.handsPlayed` count to a per-player
 * `session_entries.handsWon` count: adds the new column and drops the old one. Existing rows'
 * per-player hand counts are unknown, so `handsWon` starts NULL just as `handsPlayed` did.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE session_entries ADD COLUMN handsWon INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sessions_new (
                id TEXT NOT NULL,
                date INTEGER NOT NULL,
                status TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                isDeleted INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO sessions_new (id, date, status, createdAt, updatedAt, isDeleted)
            SELECT id, date, status, createdAt, updatedAt, isDeleted FROM sessions
            """.trimIndent()
        )
        db.execSQL("DROP TABLE sessions")
        db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
    }
}
