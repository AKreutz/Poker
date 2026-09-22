package com.akreutz.poker.ui.common

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
