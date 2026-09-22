package com.akreutz.poker.ui.common

import java.util.Locale

fun formatCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absCents = kotlin.math.abs(cents)
    return String.format(Locale.GERMANY, "%s%d,%02d", sign, absCents / 100, absCents % 100)
}
