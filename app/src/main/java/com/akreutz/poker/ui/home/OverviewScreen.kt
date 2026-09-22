package com.akreutz.poker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.akreutz.poker.ui.currentsession.CurrentSessionScreen

private enum class OverviewTab(val label: String) {
    Summary(label = "Summary"),
    CurrentSession(label = "Current session"),
}

@Composable
fun OverviewScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            OverviewTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab.label) },
                )
            }
        }

        when (OverviewTab.entries[selectedTab]) {
            OverviewTab.Summary -> HomeScreen(modifier = Modifier.fillMaxSize())
            OverviewTab.CurrentSession -> CurrentSessionScreen(modifier = Modifier.fillMaxSize())
        }
    }
}
