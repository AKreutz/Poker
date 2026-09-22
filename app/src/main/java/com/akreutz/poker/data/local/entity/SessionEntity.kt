package com.akreutz.poker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val status: SessionStatus = SessionStatus.OPEN,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isDeleted: Boolean = false,
)
