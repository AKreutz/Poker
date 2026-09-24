package com.akreutz.poker.data.model

import java.time.LocalDate

data class PlayerSessionOutcome(
    val playerName: String,
    val deltaCents: Long,
)

sealed class StreakUpdate {
    abstract val playerName: String
    abstract val length: Int
    abstract val isWinStreak: Boolean

    /** The streaking player played this session and extended their streak. */
    data class Continued(
        override val playerName: String,
        override val length: Int,
        override val isWinStreak: Boolean,
    ) : StreakUpdate()

    /** The streaking player played this session and their streak ended. */
    data class Broken(
        override val playerName: String,
        override val length: Int,
        override val isWinStreak: Boolean,
    ) : StreakUpdate()

    /** The streaking player sat out; their streak is untouched and still the longest active one. */
    data class Stood(
        override val playerName: String,
        override val length: Int,
        override val isWinStreak: Boolean,
    ) : StreakUpdate()

    /** The streaking player sat out and another player's streak (extended this session) overtook it. */
    data class TakenOver(
        val previousPlayerName: String,
        val previousLength: Int,
        override val playerName: String,
        override val length: Int,
        override val isWinStreak: Boolean,
    ) : StreakUpdate()
}

data class NewRecord(
    val label: String,
    val playerName: String,
)

data class SessionResult(
    val sessionDate: LocalDate,
    val outcomes: List<PlayerSessionOutcome>,
    val streakUpdates: List<StreakUpdate>,
    val newRecords: List<NewRecord>,
    val handsPlayed: Int? = null,
)

fun computeSessionResult(
    sessionWithEntries: SessionWithEntries,
    recordsBefore: PokerRecords?,
    recordsAfter: PokerRecords,
): SessionResult {
    val outcomes = sessionWithEntries.entries
        .sortedByDescending { it.entry.deltaCents }
        .map { PlayerSessionOutcome(it.player.name, it.entry.deltaCents) }

    val sessionDate = sessionWithEntries.session.date
    val playedNames = outcomes.map { it.playerName }.toSet()

    val streakUpdates = buildList {
        fun evaluate(before: PlayerStreak?, isWin: Boolean) {
            if (before == null) return

            val afterForSamePlayer = recordsAfter.activeStreaksByPlayer[before.playerName]
                ?.takeIf { it.isWin == isWin }
            val longestAfter = if (isWin) recordsAfter.longestActiveWinStreak else recordsAfter.longestActiveLossStreak

            if (before.playerName in playedNames) {
                if (afterForSamePlayer != null && afterForSamePlayer.length > before.length) {
                    add(StreakUpdate.Continued(before.playerName, afterForSamePlayer.length, isWin))
                } else {
                    add(StreakUpdate.Broken(before.playerName, before.length, isWin))
                }
            } else {
                if (longestAfter != null &&
                    longestAfter.playerName != before.playerName &&
                    longestAfter.length > before.length
                ) {
                    add(
                        StreakUpdate.TakenOver(
                            previousPlayerName = before.playerName,
                            previousLength = before.length,
                            playerName = longestAfter.playerName,
                            length = longestAfter.length,
                            isWinStreak = isWin,
                        )
                    )
                } else {
                    add(StreakUpdate.Stood(before.playerName, before.length, isWin))
                }
            }
        }

        evaluate(recordsBefore?.longestActiveWinStreak, isWin = true)
        evaluate(recordsBefore?.longestActiveLossStreak, isWin = false)
    }

    val newRecords = buildList {
        fun checkRecord(label: String, before: Any?, after: Any?, playerName: String?, date: LocalDate?) {
            if (playerName != null && date == sessionDate && before != after) {
                add(NewRecord(label, playerName))
            }
        }
        checkRecord("Biggest win", recordsBefore?.biggestWin, recordsAfter.biggestWin, recordsAfter.biggestWin?.playerName, recordsAfter.biggestWin?.sessionDate)
        checkRecord("Biggest loss", recordsBefore?.biggestLoss, recordsAfter.biggestLoss, recordsAfter.biggestLoss?.playerName, recordsAfter.biggestLoss?.sessionDate)
        checkRecord("Highest balance", recordsBefore?.highestBalance, recordsAfter.highestBalance, recordsAfter.highestBalance?.playerName, recordsAfter.highestBalance?.sessionDate)
        checkRecord("Lowest balance", recordsBefore?.lowestBalance, recordsAfter.lowestBalance, recordsAfter.lowestBalance?.playerName, recordsAfter.lowestBalance?.sessionDate)
        recordsAfter.longestWinStreak?.let {
            if (it.endDate == sessionDate && it != recordsBefore?.longestWinStreak) {
                add(NewRecord("Longest win streak", it.playerName))
            }
        }
        recordsAfter.longestLossStreak?.let {
            if (it.endDate == sessionDate && it != recordsBefore?.longestLossStreak) {
                add(NewRecord("Longest loss streak", it.playerName))
            }
        }
    }

    return SessionResult(sessionDate, outcomes, streakUpdates, newRecords, sessionWithEntries.session.handsPlayed)
}
