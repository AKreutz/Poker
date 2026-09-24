package com.akreutz.poker.ui.common

import java.time.Duration
import java.util.Locale

fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absCents = kotlin.math.abs(cents)
    return String.format(Locale.GERMANY, "%s%d,%02d €", sign, absCents / 100, absCents % 100)
}

fun parseCentsInput(raw: String): Long? {
    if (raw.isBlank()) return null
    val normalized = raw.trim().replace("€", "").trim().replace(",", ".")
    val value = normalized.toDoubleOrNull() ?: return null
    return Math.round(value * 100)
}

fun formatSessions(count: Int): String = "$count session${if (count == 1) "" else "s"}"

fun formatHands(count: Int): String = "$count hand${if (count == 1) "" else "s"}"

fun formatDuration(duration: Duration): String {
    val totalMinutes = duration.toMinutes()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }
}

/**
 * Sessions restored from a Drive snapshot can have their start/end instants collapsed to the
 * same import timestamp, which would otherwise show as a 0min session; anything under a minute
 * is treated as "no reliable duration" rather than displayed.
 */
val MIN_MEASURABLE_SESSION_DURATION: Duration = Duration.ofMinutes(1)

/** Null if [duration] is too short to be a real, measured session length (see above). */
fun Duration.takeIfMeasurable(): Duration? = takeIf { it >= MIN_MEASURABLE_SESSION_DURATION }
