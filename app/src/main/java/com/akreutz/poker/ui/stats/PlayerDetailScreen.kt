package com.akreutz.poker.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.model.PlayerStreak
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.data.model.SinglePlayerRecords
import com.akreutz.poker.ui.common.RecordStatTile
import com.akreutz.poker.ui.common.RecordTile
import com.akreutz.poker.ui.common.RecordTone
import com.akreutz.poker.ui.common.formatCents
import com.akreutz.poker.ui.common.formatSessions
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

private val SESSION_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)

private fun PlayerStreak.formatDateRange(): String =
    if (startDate == endDate) {
        startDate.format(SESSION_DATE_FORMATTER)
    } else {
        "${startDate.format(SESSION_DATE_FORMATTER)} – ${endDate.format(SESSION_DATE_FORMATTER)}"
    }

@Composable
fun PlayerDetailScreen(playerId: String, modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: PlayerDetailViewModel = viewModel(
        factory = PlayerDetailViewModel.Factory(application.repository, playerId),
    )
    val uiState by viewModel.uiState.collectAsState()
    val player = uiState.player

    if (player == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = player.name,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        item {
            PlayerTotalsCards(uiState.totals, uiState.standardDeviationCents)
        }
        if (uiState.records != null) {
            item {
                PlayerRecordsCard(uiState.records!!)
            }
        }
        item {
            Text(
                text = "Session history",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        if (uiState.sessionResults.isEmpty()) {
            item {
                Text(
                    text = "No concluded sessions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.sessionResults) { result ->
                SessionResultRow(result)
            }
        }
    }
}

@Composable
private fun PlayerTotalsCards(totals: PlayerTotals?, standardDeviationCents: Double?) {
    if (totals == null) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = "No concluded sessions yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    val avgPerSession = if (totals.sessionsPlayed > 0) {
        totals.totalDeltaCents.toDouble() / totals.sessionsPlayed
    } else {
        0.0
    }

    val netTile = RecordTile(
        label = "Current balance",
        value = formatCents(totals.totalDeltaCents),
        dateText = null,
        icon = if (totals.totalDeltaCents >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
        tone = if (totals.totalDeltaCents >= 0) RecordTone.POSITIVE else RecordTone.NEGATIVE,
    )
    val sessionsTile = RecordTile(
        label = "Sessions played",
        value = totals.sessionsPlayed.toString(),
        dateText = null,
        icon = Icons.Filled.EventNote,
        tone = RecordTone.NEUTRAL,
    )
    val buyInTile = RecordTile(
        label = "Total buy-in",
        value = formatCents(totals.totalBuyInCents),
        dateText = null,
        icon = Icons.Filled.Savings,
        tone = RecordTone.NEUTRAL,
    )
    val cashOutTile = RecordTile(
        label = "Total cash-out",
        value = formatCents(totals.totalCashOutCents),
        dateText = null,
        icon = Icons.Filled.MoneyOff,
        tone = RecordTone.NEUTRAL,
    )
    val avgTile = RecordTile(
        label = if (avgPerSession >= 0) "Earnings per session" else "Losses per session",
        value = formatCents(avgPerSession.roundToLong()),
        dateText = null,
        icon = if (avgPerSession >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
        tone = if (avgPerSession >= 0) RecordTone.POSITIVE else RecordTone.NEGATIVE,
    )
    val varTile = RecordTile(
        label = "Volatility per session",
        value = if (standardDeviationCents != null) {
            "± ${formatCents(standardDeviationCents.roundToLong())}"
        } else {
            "—"
        },
        dateText = null,
        icon = Icons.Filled.TrendingFlat,
        tone = RecordTone.NEUTRAL,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecordStatTile(netTile, modifier = Modifier.weight(1f))
            RecordStatTile(avgTile, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecordStatTile(sessionsTile, modifier = Modifier.weight(1f))
            RecordStatTile(varTile, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RecordStatTile(buyInTile, modifier = Modifier.weight(1f))
            RecordStatTile(cashOutTile, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayerRecordsCard(records: SinglePlayerRecords) {
    val biggestWinTile = records.biggestWin?.let {
        RecordTile(
            label = "Biggest win",
            value = formatCents(it.deltaCents),
            dateText = it.sessionDate.format(SESSION_DATE_FORMATTER),
            icon = Icons.Filled.Savings,
            tone = RecordTone.POSITIVE,
        )
    }
    val biggestLossTile = records.biggestLoss?.let {
        RecordTile(
            label = "Biggest loss",
            value = formatCents(it.deltaCents),
            dateText = it.sessionDate.format(SESSION_DATE_FORMATTER),
            icon = Icons.Filled.MoneyOff,
            tone = RecordTone.NEGATIVE,
        )
    }
    val highestBalanceTile = records.highestBalance?.let {
        RecordTile(
            label = "Highest balance",
            value = formatCents(it.balanceCents),
            dateText = it.sessionDate.format(SESSION_DATE_FORMATTER),
            icon = Icons.Filled.TrendingUp,
            tone = RecordTone.POSITIVE,
        )
    }
    val lowestBalanceTile = records.lowestBalance?.let {
        RecordTile(
            label = "Lowest balance",
            value = formatCents(it.balanceCents),
            dateText = it.sessionDate.format(SESSION_DATE_FORMATTER),
            icon = Icons.Filled.TrendingDown,
            tone = RecordTone.NEGATIVE,
        )
    }
    val longestWinStreakTile = records.longestWinStreak?.let {
        RecordTile(
            label = "Longest win streak",
            value = formatSessions(it.length),
            dateText = it.formatDateRange(),
            icon = Icons.Filled.Whatshot,
            tone = RecordTone.POSITIVE,
        )
    }
    val longestLossStreakTile = records.longestLossStreak?.let {
        RecordTile(
            label = "Longest loss streak",
            value = formatSessions(it.length),
            dateText = it.formatDateRange(),
            icon = Icons.Filled.AcUnit,
            tone = RecordTone.NEGATIVE,
        )
    }
    val activeStreakTile = records.activeStreak?.let {
        RecordTile(
            label = if (it.isWin) "Active win streak" else "Active loss streak",
            value = formatSessions(it.length),
            dateText = it.formatDateRange(),
            icon = if (it.isWin) Icons.Filled.LocalFireDepartment else Icons.Filled.AcUnit,
            tone = if (it.isWin) RecordTone.POSITIVE else RecordTone.NEGATIVE,
        )
    }

    val rows = listOf(
        biggestWinTile to biggestLossTile,
        highestBalanceTile to lowestBalanceTile,
        longestWinStreakTile to longestLossStreakTile,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (activeStreakTile != null) {
            RecordStatTile(activeStreakTile, modifier = Modifier.fillMaxWidth())
        }
        rows.forEach { (left, right) ->
            if (left == null && right == null) return@forEach
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (left != null) {
                    RecordStatTile(left, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (right != null) {
                    RecordStatTile(right, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SessionResultRow(result: PlayerSessionResult) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = result.sessionDate.format(SESSION_DATE_FORMATTER),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = formatCents(result.deltaCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        result.deltaCents > 0 -> MaterialTheme.colorScheme.primary
                        result.deltaCents < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            StatRow("Buy-in", formatCents(result.buyInCents))
            StatRow("Cash-out", formatCents(result.cashOutCents))
        }
    }
}
