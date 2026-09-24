package com.akreutz.poker.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.ui.SimpleViewModelFactory
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.ui.common.SessionCard

@Composable
fun SessionsScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: SessionsViewModel = viewModel(
        factory = SimpleViewModelFactory { SessionsViewModel(application.repository) },
    )
    val sessions by viewModel.sessions.collectAsState()
    val sessionResultsById by viewModel.sessionResultsById.collectAsState()

    if (sessions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sessions yet")
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(sessions, key = { _, session -> session.session.id }) { _, session ->
            SessionCard(
                sessionWithEntries = session,
                sessionResult = sessionResultsById[session.session.id],
                onDelete = { viewModel.deleteSession(session.session) },
            )
        }
    }
}
