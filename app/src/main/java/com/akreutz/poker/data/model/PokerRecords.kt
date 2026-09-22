package com.akreutz.poker.data.model

import java.time.LocalDate

data class PlayerSessionRecord(
    val playerName: String,
    val deltaCents: Long,
    val sessionDate: LocalDate,
)

data class PlayerStreak(
    val playerName: String,
    val length: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
)

data class PlayerBalancePoint(
    val playerName: String,
    val balanceCents: Long,
    val sessionDate: LocalDate,
)

data class PlayerAverage(
    val playerName: String,
    val averageCents: Double,
)

data class PlayerConsistency(
    val playerName: String,
    val standardDeviationCents: Double,
)

data class PokerRecords(
    val biggestWin: PlayerSessionRecord?,
    val biggestLoss: PlayerSessionRecord?,
    val longestWinStreak: PlayerStreak?,
    val longestLossStreak: PlayerStreak?,
    val longestActiveWinStreak: PlayerStreak?,
    val longestActiveLossStreak: PlayerStreak?,
    val highestBalance: PlayerBalancePoint?,
    val lowestBalance: PlayerBalancePoint?,
    val mostSessionsPlayed: PlayerWithSessionCount?,
    val mostProfitable: PlayerAverage?,
    val mostConsistent: PlayerConsistency?,
    val mostSwingy: PlayerConsistency?,
)
