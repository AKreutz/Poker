package com.akreutz.poker.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.ui.SimpleViewModelFactory

@Composable
fun SessionsDebugScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: SessionsDebugViewModel = viewModel(
        factory = SimpleViewModelFactory {
            SessionsDebugViewModel(application.repository, onPruned = application::syncNow)
        },
    )
    val sessions by viewModel.sessions.collectAsState()
    val deletedCount = sessions.count { it.isDeleted }
    val concludedCount = sessions.count { !it.isDeleted && it.status == SessionStatus.CONCLUDED }
    val openCount = sessions.count { !it.isDeleted && it.status == SessionStatus.OPEN }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Concluded: $concludedCount · Open: $openCount · Deleted: $deletedCount",
                style = MaterialTheme.typography.labelLarge,
            )
            Button(onClick = { viewModel.pruneDeleted() }) {
                Text("Prune deleted")
            }
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions yet")
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {
            items(sessions, key = { it.id }) { session ->
                val deletedSuffix = if (session.isDeleted) " (deleted)" else ""
                Text(
                    text = "${session.date} · ${session.status} · id=${session.id}$deletedSuffix",
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
