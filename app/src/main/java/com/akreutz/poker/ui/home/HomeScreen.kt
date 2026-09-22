package com.akreutz.poker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            text = "Active Streaks",
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

@Composable
private fun BalanceCard(playerBalances: List<PlayerTotals>) {
    if (playerBalances.isEmpty()) {
        return
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            playerBalances.forEachIndexed { index, playerTotals ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = playerTotals.playerName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val delta = playerTotals.totalDeltaCents
                    Text(
                        text = formatCents(delta),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            delta > 0 -> Color(0xFF2E7D32)
                            delta < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val rows = buildList {
                records.longestActiveWinStreak?.let {
                    add(RecordRow("Longest active win streak", it.playerName, "${it.length} sessions", it.formatDateRange()))
                }
                records.longestActiveLossStreak?.let {
                    add(RecordRow("Longest active loss streak", it.playerName, "${it.length} sessions", it.formatDateRange()))
                }
            }

            if (rows.isEmpty()) {
                Text(
                    text = "No active streaks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RecordRows(rows)
            }
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
