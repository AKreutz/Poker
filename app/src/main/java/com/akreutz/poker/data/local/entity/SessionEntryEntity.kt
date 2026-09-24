package com.akreutz.poker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "session_entries",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("playerId")],
)
data class SessionEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val playerId: String,
    val buyInCents: Long,
    val cashOutCents: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean = false,
    /**
     * Number of hands this player won during the session. Tracked live for sessions started
     * with this feature; null for older entries, or when recording was declined, where this
     * wasn't saved.
     */
    val handsWon: Int? = null,
) {
    val deltaCents: Long get() = cashOutCents - buyInCents
}
