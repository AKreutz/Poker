package com.akreutz.poker.data.model

import androidx.room.Embedded
import com.akreutz.poker.data.local.entity.PlayerEntity

data class PlayerWithSessionCount(
    @Embedded val player: PlayerEntity,
    val sessionsPlayed: Int,
)
