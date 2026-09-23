package com.akreutz.poker.ui.stats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.model.PlayerStreak
import com.akreutz.poker.ui.common.RECORD_POSITIVE_COLOR
import com.akreutz.poker.ui.common.formatCents
import com.akreutz.poker.ui.common.formatSessions
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

private const val PLAYERS_MIN_SESSIONS_DEFAULT = 5
private const val PLAYERS_MIN_SESSIONS_EXPANDED = 2

private val PLAYERS_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)

private enum class PlayerStatMetric(val label: String) {
    CURRENT_BALANCE("Current balance"),
    EARNINGS_PER_SESSION("Earnings per session"),
    VOLATILITY_PER_SESSION("Volatility per session"),
    ACTIVE_STREAK("Active streak"),
    BIGGEST_WIN("Biggest win"),
    BIGGEST_LOSS("Biggest loss"),
    HIGHEST_BALANCE("Highest balance"),
    LOWEST_BALANCE("Lowest balance"),
    LONGEST_WIN_STREAK("Longest win streak"),
    LONGEST_LOSS_STREAK("Longest loss streak"),
}

/** The value used for sorting: cents for money metrics, session count for streak metrics. */
private fun PlayerStatMetric.sortValue(playerStats: PlayerStats): Long? {
    val totals = playerStats.totals
    val records = playerStats.records
    return when (this) {
        PlayerStatMetric.CURRENT_BALANCE -> totals?.totalDeltaCents
        PlayerStatMetric.EARNINGS_PER_SESSION -> totals?.let {
            if (it.sessionsPlayed > 0) (it.totalDeltaCents.toDouble() / it.sessionsPlayed).roundToLong() else 0L
        }
        PlayerStatMetric.VOLATILITY_PER_SESSION -> playerStats.standardDeviationCents?.roundToLong()
        PlayerStatMetric.BIGGEST_WIN -> records?.biggestWin?.deltaCents
        PlayerStatMetric.BIGGEST_LOSS -> records?.biggestLoss?.deltaCents
        PlayerStatMetric.HIGHEST_BALANCE -> records?.highestBalance?.balanceCents
        PlayerStatMetric.LOWEST_BALANCE -> records?.lowestBalance?.balanceCents
        PlayerStatMetric.ACTIVE_STREAK -> records?.activeStreak?.length?.toLong()
        PlayerStatMetric.LONGEST_WIN_STREAK -> records?.longestWinStreak?.length?.toLong()
        PlayerStatMetric.LONGEST_LOSS_STREAK -> records?.longestLossStreak?.length?.toLong()
    }
}

private fun PlayerStatMetric.formatValue(playerStats: PlayerStats): String {
    val records = playerStats.records
    return when (this) {
        PlayerStatMetric.VOLATILITY_PER_SESSION ->
            sortValue(playerStats)?.let { "± ${formatCents(it)}" } ?: "—"
        PlayerStatMetric.ACTIVE_STREAK -> records?.activeStreak?.let {
            "${formatSessions(it.length)} ${if (it.isWin) "win streak" else "loss streak"}"
        } ?: "—"
        PlayerStatMetric.LONGEST_WIN_STREAK -> records?.longestWinStreak?.let { formatSessions(it.length) } ?: "—"
        PlayerStatMetric.LONGEST_LOSS_STREAK -> records?.longestLossStreak?.let { formatSessions(it.length) } ?: "—"
        else -> sortValue(playerStats)?.let { formatCents(it) } ?: "—"
    }
}

/** Icon shown next to the value, matching the icons used on the overview tab's Records/Active Streaks cards. */
private fun PlayerStatMetric.icon(playerStats: PlayerStats): ImageVector? {
    return when (this) {
        PlayerStatMetric.CURRENT_BALANCE, PlayerStatMetric.EARNINGS_PER_SESSION -> {
            when {
                (sortValue(playerStats) ?: 0L) > 0 -> Icons.Filled.TrendingUp
                (sortValue(playerStats) ?: 0L) < 0 -> Icons.Filled.TrendingDown
                else -> Icons.Filled.TrendingFlat
            }
        }
        PlayerStatMetric.VOLATILITY_PER_SESSION -> null
        PlayerStatMetric.ACTIVE_STREAK -> playerStats.records?.activeStreak?.let {
            if (it.isWin) Icons.Filled.LocalFireDepartment else Icons.Filled.AcUnit
        }
        PlayerStatMetric.BIGGEST_WIN -> null
        PlayerStatMetric.BIGGEST_LOSS -> null
        PlayerStatMetric.HIGHEST_BALANCE -> null
        PlayerStatMetric.LOWEST_BALANCE -> null
        PlayerStatMetric.LONGEST_WIN_STREAK -> null
        PlayerStatMetric.LONGEST_LOSS_STREAK -> null
    }
}

/**
 * Sort key for the active-streak metric: win streaks rank above loss streaks, win streaks are
 * ordered longest-first, and loss streaks are ordered shortest-first (least bad first).
 */
private fun activeStreakSortKey(playerStats: PlayerStats): Pair<Int, Int>? {
    val streak = playerStats.records?.activeStreak ?: return null
    val groupRank = if (streak.isWin) 1 else 0
    val lengthRank = if (streak.isWin) -streak.length else streak.length
    return groupRank to lengthRank
}

/** Optional date/context subtitle shown under the value, mirroring the player detail screen's record tiles. */
private fun PlayerStatMetric.dateText(playerStats: PlayerStats): String? {
    val records = playerStats.records ?: return null
    fun formatStreakRange(streak: PlayerStreak): String =
        if (streak.startDate == streak.endDate) {
            streak.startDate.format(PLAYERS_DATE_FORMATTER)
        } else {
            "${streak.startDate.format(PLAYERS_DATE_FORMATTER)} – ${streak.endDate.format(PLAYERS_DATE_FORMATTER)}"
        }
    return when (this) {
        PlayerStatMetric.BIGGEST_WIN -> records.biggestWin?.sessionDate?.format(PLAYERS_DATE_FORMATTER)
        PlayerStatMetric.BIGGEST_LOSS -> records.biggestLoss?.sessionDate?.format(PLAYERS_DATE_FORMATTER)
        PlayerStatMetric.HIGHEST_BALANCE -> records.highestBalance?.sessionDate?.format(PLAYERS_DATE_FORMATTER)
        PlayerStatMetric.LOWEST_BALANCE -> records.lowestBalance?.sessionDate?.format(PLAYERS_DATE_FORMATTER)
        PlayerStatMetric.ACTIVE_STREAK -> null
        PlayerStatMetric.LONGEST_WIN_STREAK -> records.longestWinStreak?.let { formatStreakRange(it) }
        PlayerStatMetric.LONGEST_LOSS_STREAK -> records.longestLossStreak?.let { formatStreakRange(it) }
        else -> null
    }
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier, onPlayerClick: (String) -> Unit = {}) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(application.repository),
    )
    val players by viewModel.players.collectAsState()

    if (players.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No players yet")
        }
        return
    }

    var selectedMetric by remember { mutableStateOf(PlayerStatMetric.CURRENT_BALANCE) }
    var metricMenuExpanded by remember { mutableStateOf(false) }
    var sortAscending by remember { mutableStateOf(false) }

    val rankedPlayers = if (selectedMetric == PlayerStatMetric.ACTIVE_STREAK) {
        val (withValue, withoutValue) = players.partition { activeStreakSortKey(it) != null }
        val sortedWithValue = withValue.sortedWith(
            compareByDescending<PlayerStats> { activeStreakSortKey(it)!!.first }
                .thenBy { activeStreakSortKey(it)!!.second },
        )
        val ordered = if (sortAscending) sortedWithValue.reversed() else sortedWithValue
        ordered + withoutValue
    } else {
        val (withValue, withoutValue) = players.partition { selectedMetric.sortValue(it) != null }
        val sortedWithValue = withValue.sortedBy { selectedMetric.sortValue(it) }
            .let { if (sortAscending) it else it.reversed() }
        sortedWithValue + withoutValue
    }

    var expandLevel by remember { mutableIntStateOf(0) }
    val minSessions = when (expandLevel) {
        0 -> PLAYERS_MIN_SESSIONS_DEFAULT
        1 -> PLAYERS_MIN_SESSIONS_EXPANDED
        else -> 0
    }
    val visiblePlayers = rankedPlayers.filter { it.playerWithCount.sessionsPlayed >= minSessions }
    val canExpandFurther = expandLevel < 2 && visiblePlayers.size < rankedPlayers.size
    val canCollapse = expandLevel > 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Players",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        TextButton(onClick = { metricMenuExpanded = true }) {
                            Text(selectedMetric.label)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = metricMenuExpanded,
                            onDismissRequest = { metricMenuExpanded = false },
                        ) {
                            PlayerStatMetric.entries.forEach { metric ->
                                DropdownMenuItem(
                                    text = { Text(metric.label) },
                                    onClick = {
                                        selectedMetric = metric
                                        metricMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = if (sortAscending) "Sort ascending" else "Sort descending",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        items(visiblePlayers, key = { it.playerWithCount.player.id }) { playerStats ->
            PlayerCard(
                rank = visiblePlayers.indexOf(playerStats) + 1,
                playerStats = playerStats,
                metric = selectedMetric,
                onClick = { onPlayerClick(playerStats.playerWithCount.player.id) },
                onDelete = { viewModel.deletePlayer(playerStats.playerWithCount.player) },
            )
        }
        if (canExpandFurther || canCollapse) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${visiblePlayers.size} of ${rankedPlayers.size} shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        if (canCollapse) {
                            TextButton(onClick = { expandLevel = 0 }) {
                                Text("Show less")
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                            }
                        }
                        if (canExpandFurther) {
                            TextButton(onClick = { expandLevel += 1 }) {
                                Text("Show more")
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerCard(
    rank: Int,
    playerStats: PlayerStats,
    metric: PlayerStatMetric,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val playerWithCount = playerStats.playerWithCount
    val metricValue = metric.sortValue(playerStats)
    val dateText = metric.dateText(playerStats)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { showDeleteConfirmation = true }),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = playerWithCount.player.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatSessions(playerWithCount.sessionsPlayed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (metricValue != null) {
                val signDependentTrend = metric == PlayerStatMetric.CURRENT_BALANCE ||
                    metric == PlayerStatMetric.EARNINGS_PER_SESSION
                val activeStreakIsWin = if (metric == PlayerStatMetric.ACTIVE_STREAK) {
                    playerStats.records?.activeStreak?.isWin
                } else {
                    null
                }
                val fixedTrend = when (metric) {
                    PlayerStatMetric.BIGGEST_WIN, PlayerStatMetric.HIGHEST_BALANCE, PlayerStatMetric.LONGEST_WIN_STREAK -> true
                    PlayerStatMetric.BIGGEST_LOSS, PlayerStatMetric.LOWEST_BALANCE, PlayerStatMetric.LONGEST_LOSS_STREAK -> false
                    else -> null
                }
                val isWin = activeStreakIsWin ?: fixedTrend
                val tone = when {
                    signDependentTrend -> when {
                        metricValue > 0 -> RECORD_POSITIVE_COLOR
                        metricValue < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    isWin != null -> if (isWin) RECORD_POSITIVE_COLOR else MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                val icon = metric.icon(playerStats)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tone,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = metric.formatValue(playerStats),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = tone,
                        )
                    }
                    if (dateText != null) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = "No data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete player?") },
            text = { Text("This will remove ${playerWithCount.player.name} from the players list.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
