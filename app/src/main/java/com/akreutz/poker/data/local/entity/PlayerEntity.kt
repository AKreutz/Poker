package com.akreutz.poker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean = false,
)
