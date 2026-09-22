package com.akreutz.poker.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.akreutz.poker.R

enum class PokerDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
) {
    Overview(route = "home", label = "Overview", iconRes = R.drawable.ic_playing_cards),
    Stats(route = "stats", label = "Stats", icon = Icons.Filled.BarChart),
    Sessions(route = "sessions", label = "Sessions", icon = Icons.AutoMirrored.Filled.List),
}
