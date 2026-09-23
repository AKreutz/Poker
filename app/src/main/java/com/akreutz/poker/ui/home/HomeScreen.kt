package com.akreutz.poker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.model.PlayerStreak
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.PokerRecords
import com.akreutz.poker.ui.common.formatCents
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val RECORD_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)

private data class RecordRow(val label: String, val playerName: String, val value: String, val dateText: String?)

private fun PlayerStreak.formatDateRange(): String =
    if (startDate == endDate) {
        startDate.format(RECORD_DATE_FORMATTER)
    } else {
        "${startDate.format(RECORD_DATE_FORMATTER)} – ${endDate.format(RECORD_DATE_FORMATTER)}"
    }

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(application.repository),
    )
    val playerBalances by viewModel.playerBalances.collectAsState()
    val records by viewModel.records.collectAsState()

    if (records == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sessions yet")
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (playerBalances.isNotEmpty()) {
            Text(
                text = "Balance",
                style = MaterialTheme.typography.titleMedium,
            )
            Column(modifier = Modifier.padding(top = 12.dp)) {
                BalanceCard(playerBalances)
            }
        }

        Text(
            text = "Longest Active Streaks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            ActiveStreaksCard(records!!)
        }

        Text(
            text = "Records",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            RecordsCard(records!!)
        }
    }
}

private const val BALANCE_CARD_MIN_SESSIONS_DEFAULT = 5
private const val BALANCE_CARD_MIN_SESSIONS_EXPANDED = 2
private val POSITIVE_COLOR = Color(0xFF2E7D32)
private val POSITIVE_CONTAINER_COLOR = Color(0xFFDCEDC8)
private val POSITIVE_BADGE_COLOR = Color(0xFFC5E1A5)

@Composable
private fun BalanceCard(playerBalances: List<PlayerTotals>) {
    if (playerBalances.isEmpty()) {
        return
    }

    var expandLevel by remember { mutableIntStateOf(0) }

    val minSessions = when (expandLevel) {
        0 -> BALANCE_CARD_MIN_SESSIONS_DEFAULT
        1 -> BALANCE_CARD_MIN_SESSIONS_EXPANDED
        else -> 0
    }
    val visibleBalances = playerBalances
        .filter { it.sessionsPlayed >= minSessions }
        .sortedByDescending { it.totalDeltaCents }
    val canExpandFurther = expandLevel < 2 && visibleBalances.size < playerBalances.size
    val canCollapse = expandLevel > 0
    val maxAbsDelta = visibleBalances.maxOfOrNull { abs(it.totalDeltaCents) } ?: 0L

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            visibleBalances.forEachIndexed { index, playerTotals ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                BalanceRow(
                    rank = index + 1,
                    playerTotals = playerTotals,
                    maxAbsDelta = maxAbsDelta,
                )
            }
            if (canExpandFurther || canCollapse) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${visibleBalances.size} of ${playerBalances.size} shown",
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

@Composable
private fun BalanceRow(
    rank: Int,
    playerTotals: PlayerTotals,
    maxAbsDelta: Long,
) {
    val delta = playerTotals.totalDeltaCents
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = playerTotals.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatCents(delta),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        delta > 0 -> POSITIVE_COLOR
                        delta < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            DivergingBar(
                delta = delta,
                maxAbsDelta = maxAbsDelta,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(6.dp),
            )
        }
    }
}

@Composable
private fun DivergingBar(delta: Long, maxAbsDelta: Long, modifier: Modifier = Modifier) {
    val fraction = if (maxAbsDelta == 0L) 0f else (abs(delta).toFloat() / maxAbsDelta.toFloat()).coerceIn(0f, 1f)
    Row(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        // Left half: positive bars are drawn here so their bar sits flush against the
        // center line and grows outward (to the left) as the fraction increases.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (delta < 0 && fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (delta > 0 && fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                        .background(POSITIVE_COLOR),
                )
            }
        }
    }
}

@Composable
private fun RecordsCard(records: PokerRecords) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val rows = buildList {
                records.mostProfitable?.let {
                    add(RecordRow("Most profitable", it.playerName, "${formatCents(it.averageCents.roundToLong())} / session", null))
                }
                records.mostConsistent?.let {
                    add(RecordRow("Most consistent", it.playerName, "± ${formatCents(it.standardDeviationCents.roundToLong())}", null))
                }
                records.mostSwingy?.let {
                    add(RecordRow("Most swingy", it.playerName, "± ${formatCents(it.standardDeviationCents.roundToLong())}", null))
                }
                records.biggestWin?.let {
                    add(RecordRow("Biggest win", it.playerName, formatCents(it.deltaCents), it.sessionDate.format(RECORD_DATE_FORMATTER)))
                }
                records.biggestLoss?.let {
                    add(RecordRow("Biggest loss", it.playerName, formatCents(it.deltaCents), it.sessionDate.format(RECORD_DATE_FORMATTER)))
                }
                records.highestBalance?.let {
                    add(RecordRow("Highest balance", it.playerName, formatCents(it.balanceCents), it.sessionDate.format(RECORD_DATE_FORMATTER)))
                }
                records.lowestBalance?.let {
                    add(RecordRow("Lowest balance", it.playerName, formatCents(it.balanceCents), it.sessionDate.format(RECORD_DATE_FORMATTER)))
                }
                records.longestWinStreak?.let {
                    add(RecordRow("Longest win streak", it.playerName, "${it.length} sessions", it.formatDateRange()))
                }
                records.longestLossStreak?.let {
                    add(RecordRow("Longest loss streak", it.playerName, "${it.length} sessions", it.formatDateRange()))
                }
            }

            RecordRows(rows)
        }
    }
}

@Composable
private fun ActiveStreaksCard(records: PokerRecords) {
    val streaks = listOfNotNull(records.longestActiveWinStreak, records.longestActiveLossStreak)

    if (streaks.isEmpty()) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "No active streaks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        streaks.forEach { streak -> ActiveStreakTile(streak) }
    }
}

@Composable
private fun ActiveStreakTile(streak: PlayerStreak) {
    val accentColor = if (streak.isWin) POSITIVE_COLOR else MaterialTheme.colorScheme.error
    val containerColor = if (streak.isWin) POSITIVE_CONTAINER_COLOR else MaterialTheme.colorScheme.errorContainer
    val badgeColor = if (streak.isWin) {
        POSITIVE_BADGE_COLOR
    } else {
        lerp(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, 0.5f)
    }
    val label = if (streak.isWin) "Active win streak" else "Active loss streak"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (streak.isWin) Icons.Filled.LocalFireDepartment else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = streak.playerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = streak.formatDateRange(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${streak.length}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun RecordRows(rows: List<RecordRow>) {
    rows.forEachIndexed { index, row ->
        if (index > 0) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.playerName,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (row.dateText != null) {
                        Text(
                            text = row.dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
