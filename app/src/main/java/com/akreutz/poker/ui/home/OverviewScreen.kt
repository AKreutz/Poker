package com.akreutz.poker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.ui.currentsession.CurrentSessionScreen

private enum class OverviewTab(val label: String) {
    Summary(label = "Summary"),
    CurrentSession(label = "Current session"),
}

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    var selectedTab by remember { mutableIntStateOf(0) }
    val openSession by application.repository.observeOpenSession().collectAsState(initial = null)
    val hasOpenSession = openSession != null

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            OverviewTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tab.label)
                            if (tab == OverviewTab.CurrentSession && hasOpenSession && selectedTab != index) {
                                Spacer(modifier = Modifier.size(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                            }
                        }
                    },
                )
            }
        }

        when (OverviewTab.entries[selectedTab]) {
            OverviewTab.Summary -> HomeScreen(modifier = Modifier.fillMaxSize())
            OverviewTab.CurrentSession -> CurrentSessionScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
