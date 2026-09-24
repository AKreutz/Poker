package com.akreutz.poker.data.sync.drive

import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.PurgedIdEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.sync.PokerSnapshot
import kotlinx.serialization.Serializable

/**
 * JSON-friendly mirror of [PokerSnapshot]. Room entities use [java.time] types that
 * kotlinx.serialization doesn't handle out of the box, so this DTO layer converts to/from
 * plain strings/longs for the wire format stored in the Drive file.
 */
@Serializable
data class DriveSnapshotDto(
    val players: List<PlayerDto>,
    val sessions: List<SessionDto>,
    val entries: List<SessionEntryDto>,
    val purgedIds: List<PurgedIdDto> = emptyList(),
) {
    @Serializable
    data class PlayerDto(
        val id: String,
        val name: String,
        val createdAt: String,
        val updatedAt: String,
        val isDeleted: Boolean,
    )

    @Serializable
    data class SessionDto(
        val id: String,
        val date: String,
        val status: String,
        val createdAt: String,
        val updatedAt: String,
        val isDeleted: Boolean,
        val handsPlayed: Int? = null,
    )

    @Serializable
    data class SessionEntryDto(
        val id: String,
        val sessionId: String,
        val playerId: String,
        val buyInCents: Long,
        val cashOutCents: Long,
        val createdAt: String,
        val updatedAt: String,
        val isDeleted: Boolean,
    )

    @Serializable
    data class PurgedIdDto(
        val id: String,
        val purgedAt: String,
    )

    companion object {
        fun fromSnapshot(snapshot: PokerSnapshot): DriveSnapshotDto = DriveSnapshotDto(
            players = snapshot.players.map {
                PlayerDto(it.id, it.name, it.createdAt.toString(), it.updatedAt.toString(), it.isDeleted)
            },
            sessions = snapshot.sessions.map {
                SessionDto(
                    it.id, it.date.toString(), it.status.name, it.createdAt.toString(), it.updatedAt.toString(),
                    it.isDeleted, it.handsPlayed,
                )
            },
            entries = snapshot.entries.map {
                SessionEntryDto(
                    it.id, it.sessionId, it.playerId, it.buyInCents, it.cashOutCents,
                    it.createdAt.toString(), it.updatedAt.toString(), it.isDeleted,
                )
            },
            purgedIds = snapshot.purgedIds.map {
                PurgedIdDto(it.id, it.purgedAt.toString())
            },
        )
    }

    fun toSnapshot(): PokerSnapshot = PokerSnapshot(
        players = players.map {
            PlayerEntity(
                id = it.id,
                name = it.name,
                createdAt = java.time.Instant.parse(it.createdAt),
                updatedAt = java.time.Instant.parse(it.updatedAt),
                isDeleted = it.isDeleted,
            )
        },
        sessions = sessions.map {
            SessionEntity(
                id = it.id,
                date = java.time.LocalDate.parse(it.date),
                status = SessionStatus.valueOf(it.status),
                createdAt = java.time.Instant.parse(it.createdAt),
                updatedAt = java.time.Instant.parse(it.updatedAt),
                isDeleted = it.isDeleted,
                handsPlayed = it.handsPlayed,
            )
        },
        entries = entries.map {
            SessionEntryEntity(
                id = it.id,
                sessionId = it.sessionId,
                playerId = it.playerId,
                buyInCents = it.buyInCents,
                cashOutCents = it.cashOutCents,
                createdAt = java.time.Instant.parse(it.createdAt),
                updatedAt = java.time.Instant.parse(it.updatedAt),
                isDeleted = it.isDeleted,
            )
        },
        purgedIds = purgedIds.map {
            PurgedIdEntity(id = it.id, purgedAt = java.time.Instant.parse(it.purgedAt))
        },
    )
}
