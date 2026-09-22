package com.akreutz.poker.data.model

data class PlayerTotals(
    val playerId: String,
    val playerName: String,
    val sessionsPlayed: Int,
    val totalBuyInCents: Long,
    val totalCashOutCents: Long,
    val totalDeltaCents: Long,
)
