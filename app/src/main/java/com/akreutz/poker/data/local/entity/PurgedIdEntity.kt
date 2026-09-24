package com.akreutz.poker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A tombstone for a row that was permanently (hard-)deleted, keyed by the id of whatever it
 * used to point at (a [PlayerEntity], [SessionEntity] or [SessionEntryEntity] id - ids are
 * UUIDs so collisions across entity types are not a concern).
 *
 * This table exists solely so hard-deletes survive sync. [SessionEntity]/[PlayerEntity]/
 * [SessionEntryEntity] deletes are normally soft (`isDeleted = true`), because the sync merge
 * in `SyncManager` is a union keyed by id: a row simply missing from one side is assumed to be
 * "hasn't synced yet", not "was deliberately removed", so it would otherwise come back the next
 * time any device (including this one, from a stale pull) merges in a snapshot that still has
 * it. Recording the id here - and syncing this table the same way as the others, as a
 * union that only ever grows - lets the merge step permanently drop any row whose id appears
 * in the merged purge log, regardless of which side it came from.
 */
@Entity(tableName = "purged_ids")
data class PurgedIdEntity(
    @PrimaryKey val id: String,
    val purgedAt: Instant,
)
