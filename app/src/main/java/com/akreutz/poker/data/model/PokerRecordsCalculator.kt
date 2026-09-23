package com.akreutz.poker.data.model

import java.time.LocalDate

fun computePokerRecords(sessions: List<SessionWithEntries>): PokerRecords {
    val chronological = sessions.sortedBy { it.session.date }

    var biggestWin: PlayerSessionRecord? = null
    var biggestLoss: PlayerSessionRecord? = null
    val runningBalanceCents = mutableMapOf<String, Long>()
    val currentStreakPlayer = mutableMapOf<String, Boolean>()
    val currentStreakLength = mutableMapOf<String, Int>()
    val currentStreakStart = mutableMapOf<String, LocalDate>()
    val currentStreakEnd = mutableMapOf<String, LocalDate>()
    var longestWinStreak: PlayerStreak? = null
    var longestLossStreak: PlayerStreak? = null
    var highestBalance: PlayerBalancePoint? = null
    var lowestBalance: PlayerBalancePoint? = null
    val sessionsPlayed = mutableMapOf<String, Int>()
    val playerDeltas = mutableMapOf<String, MutableList<Long>>()

    for (sessionWithEntries in chronological) {
        val date = sessionWithEntries.session.date
        for (entryWithPlayer in sessionWithEntries.entries) {
            val name = entryWithPlayer.player.name
            val delta = entryWithPlayer.entry.deltaCents

            if (biggestWin == null || delta > biggestWin.deltaCents) {
                biggestWin = PlayerSessionRecord(name, delta, date)
            }
            if (biggestLoss == null || delta < biggestLoss.deltaCents) {
                biggestLoss = PlayerSessionRecord(name, delta, date)
            }

            sessionsPlayed[name] = (sessionsPlayed[name] ?: 0) + 1
            playerDeltas.getOrPut(name) { mutableListOf() }.add(delta)

            val isWin = delta > 0
            val isLoss = delta < 0
            if (isWin || isLoss) {
                val sameDirection = currentStreakPlayer[name] == isWin
                val newLength = if (sameDirection) (currentStreakLength[name] ?: 0) + 1 else 1
                val streakStart = if (sameDirection) currentStreakStart[name] ?: date else date
                currentStreakPlayer[name] = isWin
                currentStreakLength[name] = newLength
                currentStreakStart[name] = streakStart
                currentStreakEnd[name] = date

                if (isWin && (longestWinStreak == null || newLength > longestWinStreak.length)) {
                    longestWinStreak = PlayerStreak(name, newLength, streakStart, date, isWin = true)
                }
                if (isLoss && (longestLossStreak == null || newLength > longestLossStreak.length)) {
                    longestLossStreak = PlayerStreak(name, newLength, streakStart, date, isWin = false)
                }
            } else {
                currentStreakPlayer.remove(name)
                currentStreakLength.remove(name)
                currentStreakStart.remove(name)
                currentStreakEnd.remove(name)
            }

            val newBalance = (runningBalanceCents[name] ?: 0L) + delta
            runningBalanceCents[name] = newBalance

            if (highestBalance == null || newBalance > highestBalance.balanceCents) {
                highestBalance = PlayerBalancePoint(name, newBalance, date)
            }
            if (lowestBalance == null || newBalance < lowestBalance.balanceCents) {
                lowestBalance = PlayerBalancePoint(name, newBalance, date)
            }
        }
    }

    val activeStreaksByPlayer = currentStreakPlayer.keys.associateWith { name ->
        PlayerStreak(
            playerName = name,
            length = currentStreakLength.getValue(name),
            startDate = currentStreakStart.getValue(name),
            endDate = currentStreakEnd.getValue(name),
            isWin = currentStreakPlayer.getValue(name),
        )
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

    val mostProfitable = playerDeltas
        .mapValues { (_, deltas) -> deltas.average() }
        .maxByOrNull { it.value }
        ?.let { (name, average) -> PlayerAverage(name, average) }

    val playerStandardDeviations = playerDeltas
        .filterValues { it.size >= 2 }
        .mapValues { (_, deltas) -> deltas.standardDeviation() }

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

private fun List<Long>.standardDeviation(): Double {
    val mean = average()
    val variance = sumOf { (it - mean) * (it - mean) } / size
    return kotlin.math.sqrt(variance)
}
