package com.akreutz.poker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.akreutz.poker.data.model.PlayerTotals
import com.akreutz.poker.ui.common.StaticSessionCard
import com.akreutz.poker.ui.common.formatCents

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(application.repository),
    )
    val mostRecentSession by viewModel.mostRecentSession.collectAsState()
    val playerBalances by viewModel.playerBalances.collectAsState()

    val session = mostRecentSession
    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sessions yet")
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Balance",
            style = MaterialTheme.typography.titleMedium,
        )
        Column(modifier = Modifier.padding(top = 12.dp)) {
            BalanceCard(playerBalances)
        }

        Text(
            text = "Most Recent Session",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp),
        )
        Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StaticSessionCard(session)
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
