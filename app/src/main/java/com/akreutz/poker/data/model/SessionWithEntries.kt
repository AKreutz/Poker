package com.akreutz.poker.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity

data class SessionEntryWithPlayer(
    @Embedded val entry: SessionEntryEntity,
    @Relation(parentColumn = "playerId", entityColumn = "id")
    val player: PlayerEntity,
)

data class SessionWithEntries(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
        entity = SessionEntryEntity::class,
    )
    val entries: List<SessionEntryWithPlayer>,
)
