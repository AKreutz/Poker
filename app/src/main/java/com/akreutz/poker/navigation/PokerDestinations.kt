package com.akreutz.poker.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

enum class PokerDestination(val route: String, val label: String, val icon: ImageVector) {
    Home(route = "home", label = "Home", icon = Icons.Filled.Home),
    Stats(route = "stats", label = "Stats", icon = Icons.Filled.BarChart),
    Sessions(route = "sessions", label = "Sessions", icon = Icons.AutoMirrored.Filled.List),
}
