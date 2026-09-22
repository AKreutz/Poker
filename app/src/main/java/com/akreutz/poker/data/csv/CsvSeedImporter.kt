package com.akreutz.poker.data.csv

import android.content.Context
import androidx.room.withTransaction
import com.akreutz.poker.data.local.AppDatabase
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val ASSET_FILE_NAME = "Poker.csv"
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private const val COLUMNS_PER_PLAYER = 3

class CsvSeedImporter(
    private val context: Context,
    private val database: AppDatabase,
) {
    suspend fun seedIfNeeded() {
        val lines = context.assets.open(ASSET_FILE_NAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readLines() }
        if (lines.size < 2) return

        val header = lines[0].split(";")
        val playerNames = mutableListOf<String>()
        var i = 1
        while (i < header.size) {
            val name = header[i].trim()
            if (name.isEmpty()) break
            playerNames += name
            i += COLUMNS_PER_PLAYER
        }

        database.withTransaction {
            val playerCache = mutableMapOf<String, PlayerEntity>()
            for (name in playerNames) {
                playerCache[name] = getOrCreatePlayer(name)
            }

            for (lineIndex in 2 until lines.size) {
                val row = lines[lineIndex].split(";")
                if (row.isEmpty()) continue

                val date = parseDate(row.getOrNull(0)?.trim()) ?: continue

                var session: SessionEntity? = null
                for ((playerIndex, playerName) in playerNames.withIndex()) {
                    val startCol = 1 + playerIndex * COLUMNS_PER_PLAYER
                    val endeCol = startCol + 1
                    val startRaw = row.getOrNull(startCol)?.trim().orEmpty()
                    val endeRaw = row.getOrNull(endeCol)?.trim().orEmpty()

                    if (startRaw.isEmpty() && endeRaw.isEmpty()) continue

                    val buyInCents = parseCents(startRaw) ?: continue
                    val cashOutCents = if (endeRaw.isEmpty()) 0L else (parseCents(endeRaw) ?: 0L)

                    if (session == null) {
                        session = SessionEntity(
                            date = date,
                            status = SessionStatus.CONCLUDED,
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        )
                        database.sessionDao().insert(session)
                    }

                    val player = playerCache.getValue(playerName)
                    database.sessionEntryDao().insert(
                        SessionEntryEntity(
                            sessionId = session.id,
                            playerId = player.id,
                            buyInCents = buyInCents,
                            cashOutCents = cashOutCents,
                            createdAt = Instant.now(),
                            updatedAt = Instant.now(),
                        )
                    )
                }
            }
        }
    }

    private suspend fun getOrCreatePlayer(name: String): PlayerEntity {
        val dao = database.playerDao()
        dao.findByName(name)?.let { return it }
        val now = Instant.now()
        val player = PlayerEntity(name = name, createdAt = now, updatedAt = now)
        dao.insert(player)
        return player
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(raw, DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun parseCents(raw: String): Long? {
        if (raw.isEmpty()) return null
        val normalized = raw.replace(",", ".")
        val value = normalized.toDoubleOrNull() ?: return null
        return Math.round(value * 100)
    }
}
