package com.akreutz.poker.data.model

import com.akreutz.poker.data.local.entity.SessionStatus
import java.time.LocalDate

/** One player's session result, used as input to per-player record calculation. */
data class PlayerSessionDelta(
    val sessionDate: LocalDate,
    val deltaCents: Long,
)

/** Records derived from a single player's chronological session results. */
data class SinglePlayerRecords(
    val biggestWin: PlayerSessionDelta?,
    val biggestLoss: PlayerSessionDelta?,
    val highestBalance: PlayerBalancePoint?,
    val lowestBalance: PlayerBalancePoint?,
    val longestWinStreak: PlayerStreak?,
    val longestLossStreak: PlayerStreak?,
    val activeStreak: PlayerStreak?,
)

/**
 * Walks one player's session deltas in chronological order, tracking running balance and
 * win/loss streaks. Shared by the all-players [computePokerRecords] (which runs this per
 * player and keeps only the overall best/worst) and per-player detail screens (which keep
 * the full result).
 */
fun computeSinglePlayerRecords(playerName: String, deltasChronological: List<PlayerSessionDelta>): SinglePlayerRecords? {
    if (deltasChronological.isEmpty()) return null

    var biggestWin: PlayerSessionDelta? = null
    var biggestLoss: PlayerSessionDelta? = null
    var highestBalance: PlayerBalancePoint? = null
    var lowestBalance: PlayerBalancePoint? = null
    var longestWinStreak: PlayerStreak? = null
    var longestLossStreak: PlayerStreak? = null

    var runningBalance = 0L
    var streakIsWin: Boolean? = null
    var streakLength = 0
    var streakStart: LocalDate? = null
    var streakEnd: LocalDate? = null

    for (result in deltasChronological) {
        val delta = result.deltaCents
        val date = result.sessionDate

        if (biggestWin == null || delta > biggestWin.deltaCents) biggestWin = result
        if (biggestLoss == null || delta < biggestLoss.deltaCents) biggestLoss = result

        runningBalance += delta
        if (highestBalance == null || runningBalance > highestBalance.balanceCents) {
            highestBalance = PlayerBalancePoint(playerName, runningBalance, date)
        }
        if (lowestBalance == null || runningBalance < lowestBalance.balanceCents) {
            lowestBalance = PlayerBalancePoint(playerName, runningBalance, date)
        }

        val isWin = delta > 0
        val isLoss = delta < 0
        if (isWin || isLoss) {
            val sameDirection = streakIsWin == isWin
            streakLength = if (sameDirection) streakLength + 1 else 1
            streakStart = if (sameDirection) streakStart ?: date else date
            streakIsWin = isWin
            streakEnd = date

            if (isWin && (longestWinStreak == null || streakLength > longestWinStreak.length)) {
                longestWinStreak = PlayerStreak(playerName, streakLength, streakStart, date, isWin = true)
            }
            if (isLoss && (longestLossStreak == null || streakLength > longestLossStreak.length)) {
                longestLossStreak = PlayerStreak(playerName, streakLength, streakStart, date, isWin = false)
            }
        } else {
            streakIsWin = null
            streakLength = 0
            streakStart = null
            streakEnd = null
        }
    }

    val activeStreak = if (streakIsWin != null && streakStart != null && streakEnd != null) {
        PlayerStreak(playerName, streakLength, streakStart, streakEnd, isWin = streakIsWin)
    } else {
        null
    }

    return SinglePlayerRecords(
        biggestWin = biggestWin,
        biggestLoss = biggestLoss,
        highestBalance = highestBalance,
        lowestBalance = lowestBalance,
        longestWinStreak = longestWinStreak,
        longestLossStreak = longestLossStreak,
        activeStreak = activeStreak,
    )
}

/**
 * Walks concluded sessions in chronological order and groups each player's session deltas by
 * player id. Shared by every screen that needs a player's result history: the all-players
 * [computePokerRecords], per-player detail/stat screens, and volatility calculations.
 */
fun chronologicalDeltasByPlayerId(sessions: List<SessionWithEntries>): Map<String, List<PlayerSessionDelta>> =
    sessions
        .filter { it.session.status == SessionStatus.CONCLUDED }
        .sortedBy { it.session.date }
        .flatMap { sessionWithEntries ->
            sessionWithEntries.entries.map { entryWithPlayer ->
                entryWithPlayer.entry.playerId to PlayerSessionDelta(
                    sessionDate = sessionWithEntries.session.date,
                    deltaCents = entryWithPlayer.entry.deltaCents,
                )
            }
        }
        .groupBy({ it.first }, { it.second })

fun computePokerRecords(sessions: List<SessionWithEntries>): PokerRecords {
    val chronological = sessions.sortedBy { it.session.date }

    val deltasByPlayer = mutableMapOf<String, MutableList<PlayerSessionDelta>>()
    val sessionsPlayed = mutableMapOf<String, Int>()

    for (sessionWithEntries in chronological) {
        val date = sessionWithEntries.session.date
        for (entryWithPlayer in sessionWithEntries.entries) {
            val name = entryWithPlayer.player.name
            val delta = entryWithPlayer.entry.deltaCents
            deltasByPlayer.getOrPut(name) { mutableListOf() }.add(PlayerSessionDelta(date, delta))
            sessionsPlayed[name] = (sessionsPlayed[name] ?: 0) + 1
        }
    }

    val recordsByPlayer = deltasByPlayer.mapValues { (name, deltas) -> computeSinglePlayerRecords(name, deltas) }

    var biggestWin: PlayerSessionRecord? = null
    var biggestLoss: PlayerSessionRecord? = null
    var highestBalance: PlayerBalancePoint? = null
    var lowestBalance: PlayerBalancePoint? = null
    var longestWinStreak: PlayerStreak? = null
    var longestLossStreak: PlayerStreak? = null
    val activeStreaksByPlayer = mutableMapOf<String, PlayerStreak>()

    for ((name, playerRecords) in recordsByPlayer) {
        if (playerRecords == null) continue

        playerRecords.biggestWin?.let {
            if (biggestWin == null || it.deltaCents > biggestWin!!.deltaCents) {
                biggestWin = PlayerSessionRecord(name, it.deltaCents, it.sessionDate)
            }
        }
        playerRecords.biggestLoss?.let {
            if (biggestLoss == null || it.deltaCents < biggestLoss!!.deltaCents) {
                biggestLoss = PlayerSessionRecord(name, it.deltaCents, it.sessionDate)
            }
        }
        playerRecords.highestBalance?.let {
            if (highestBalance == null || it.balanceCents > highestBalance!!.balanceCents) {
                highestBalance = it
            }
        }
        playerRecords.lowestBalance?.let {
            if (lowestBalance == null || it.balanceCents < lowestBalance!!.balanceCents) {
                lowestBalance = it
            }
        }
        playerRecords.longestWinStreak?.let {
            if (longestWinStreak == null || it.length > longestWinStreak!!.length) longestWinStreak = it
        }
        playerRecords.longestLossStreak?.let {
            if (longestLossStreak == null || it.length > longestLossStreak!!.length) longestLossStreak = it
        }
        playerRecords.activeStreak?.let { activeStreaksByPlayer[name] = it }
    }

    val longestActiveWinStreak = activeStreaksByPlayer.values
        .filter { it.isWin }
        .maxByOrNull { it.length }

    val longestActiveLossStreak = activeStreaksByPlayer.values
        .filterNot { it.isWin }
        .maxByOrNull { it.length }

    val mostSessionsPlayed = sessionsPlayed.maxByOrNull { it.value }?.let { (name, count) ->
        PlayerWithSessionCount(
            player = chronological
                .flatMap { it.entries }
                .first { it.player.name == name }
                .player,
            sessionsPlayed = count,
        )
    }

    val mostProfitable = deltasByPlayer
        .mapValues { (_, deltas) -> deltas.map { it.deltaCents }.average() }
        .maxByOrNull { it.value }
        ?.let { (name, average) -> PlayerAverage(name, average) }

    val playerStandardDeviations = deltasByPlayer
        .filterValues { it.size >= 2 }
        .mapValues { (_, deltas) -> deltas.map { it.deltaCents }.standardDeviation() }

    val mostConsistent = playerStandardDeviations
        .minByOrNull { it.value }
        ?.let { (name, stdDev) -> PlayerConsistency(name, stdDev) }

    val mostSwingy = playerStandardDeviations
        .maxByOrNull { it.value }
        ?.let { (name, stdDev) -> PlayerConsistency(name, stdDev) }

    return PokerRecords(
        biggestWin = biggestWin,
        biggestLoss = biggestLoss,
        longestWinStreak = longestWinStreak,
        longestLossStreak = longestLossStreak,
        longestActiveWinStreak = longestActiveWinStreak,
        longestActiveLossStreak = longestActiveLossStreak,
        activeStreaksByPlayer = activeStreaksByPlayer,
        highestBalance = highestBalance,
        lowestBalance = lowestBalance,
        mostSessionsPlayed = mostSessionsPlayed,
        mostProfitable = mostProfitable,
        mostConsistent = mostConsistent,
        mostSwingy = mostSwingy,
    )
}

fun List<Long>.standardDeviation(): Double {
    val mean = average()
    val variance = sumOf { (it - mean) * (it - mean) } / size
    return kotlin.math.sqrt(variance)
}
