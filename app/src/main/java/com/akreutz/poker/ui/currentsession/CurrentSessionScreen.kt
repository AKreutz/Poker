package com.akreutz.poker.ui.currentsession

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.akreutz.poker.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.model.PlayerWithSessionCount
import com.akreutz.poker.data.model.SessionEntryWithPlayer
import com.akreutz.poker.data.model.SessionResult
import com.akreutz.poker.data.model.SessionWithEntries
import com.akreutz.poker.ui.common.SessionHighlights
import com.akreutz.poker.ui.common.formatCents
import com.akreutz.poker.ui.common.parseCentsInput
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun CurrentSessionScreen(modifier: Modifier = Modifier) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: CurrentSessionViewModel = viewModel(
        factory = CurrentSessionViewModel.Factory(application.repository),
    )
    val openSession by viewModel.openSession.collectAsState()
    val showPlayerSelection by viewModel.showPlayerSelection.collectAsState()
    val showConcludeDialog by viewModel.showConcludeDialog.collectAsState()
    val players by viewModel.players.collectAsState()
    val sessionResult by viewModel.sessionResult.collectAsState()

    if (showPlayerSelection) {
        PlayerSelectionDialog(
            players = players,
            onConfirm = { selectedIds, newPlayerNames, buyInCents ->
                viewModel.confirmPlayerSelection(selectedIds, newPlayerNames, buyInCents)
            },
            onDismiss = { viewModel.dismissPlayerSelection() },
        )
    }

    if (showConcludeDialog) {
        openSession?.let { session ->
            ConcludeSessionDialog(
                sessionWithEntries = session,
                onConfirm = { cashOuts -> viewModel.confirmConclude(cashOuts) },
                onDismiss = { viewModel.dismissConcludeDialog() },
            )
        }
    }

    sessionResult?.let { result ->
        SessionResultDialog(
            result = result,
            onDismiss = { viewModel.dismissSessionResult() },
        )
    }

    val session = openSession
    if (session == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "No active session",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Start a session to track buy-ins and cash-outs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.size(16.dp))
                Button(
                    onClick = { viewModel.startSession() },
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Start session", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        return
    }

    var showCancelConfirmation by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PlayerBuyInList(
            sessionWithEntries = session,
            onIncreaseBuyIn = { entry, additionalCents -> viewModel.increaseBuyIn(entry, additionalCents) },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { showCancelConfirmation = true },
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel")
            }
            Button(
                onClick = { viewModel.concludeSession() },
                modifier = Modifier.weight(1f),
            ) {
                Text("Conclude session")
            }
        }
    }

    if (showCancelConfirmation) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Cancel session?") },
            text = { Text("This will discard the current session and everything recorded in it.") },
            confirmButton = {
                Button(onClick = {
                    showCancelConfirmation = false
                    viewModel.cancelSession()
                }) {
                    Text("Discard session")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirmation = false }) {
                    Text("Keep session")
                }
            },
        )
    }
}

@Composable
private fun PlayerBuyInList(
    sessionWithEntries: SessionWithEntries,
    onIncreaseBuyIn: (SessionEntryEntity, Long) -> Unit,
) {
    var entryForBuyInDialog by remember { mutableStateOf<SessionEntryWithPlayer?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Player",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Buy-in",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        sessionWithEntries.entries.forEachIndexed { index, entryWithPlayer ->
            if (index > 0) {
                HorizontalDivider()
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entryWithPlayer.player.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatCents(entryWithPlayer.entry.buyInCents),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32),
                    )
                    IconButton(
                        onClick = { entryForBuyInDialog = entryWithPlayer },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase buy-in for ${entryWithPlayer.player.name}")
                    }
                }
            }
        }
    }

    val target = entryForBuyInDialog
    if (target != null) {
        AddBuyInDialog(
            playerName = target.player.name,
            onConfirm = { additionalCents ->
                onIncreaseBuyIn(target.entry, additionalCents)
                entryForBuyInDialog = null
            },
            onDismiss = { entryForBuyInDialog = null },
        )
    }
}

@Composable
private fun AddBuyInDialog(
    playerName: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    val amountCents = parseCentsInput(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add buy-in for $playerName") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                isError = amountCents == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { amountCents?.let(onConfirm) },
                enabled = amountCents != null && amountCents > 0,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ConcludeSessionDialog(
    sessionWithEntries: SessionWithEntries,
    onConfirm: (Map<String, Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = sessionWithEntries.entries
    var cashOutTexts by remember {
        mutableStateOf(entries.associate { it.entry.id to "" })
    }
    val cashOutCentsById = entries.associate { it.entry.id to parseCentsInput(cashOutTexts[it.entry.id].orEmpty()) }
    val allValid = entries.isNotEmpty() && cashOutCentsById.values.all { it != null }
    val differenceCents = if (allValid) {
        cashOutCentsById.values.sumOf { it ?: 0L } - entries.sumOf { it.entry.buyInCents }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conclude session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter each player's final stack",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entries.chunked(2).forEach { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowEntries.forEach { entryWithPlayer ->
                            val entryId = entryWithPlayer.entry.id
                            OutlinedTextField(
                                value = cashOutTexts[entryId].orEmpty(),
                                onValueChange = { cashOutTexts = cashOutTexts + (entryId to it) },
                                label = { Text(entryWithPlayer.player.name) },
                                isError = cashOutCentsById[entryId] == null,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowEntries.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Difference",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = differenceCents?.let(::formatCents) ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (differenceCents == 0L) {
                            Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cashOuts = cashOutCentsById.mapNotNull { (id, cents) ->
                        cents?.let { id to it }
                    }.toMap()
                    onConfirm(cashOuts)
                },
                enabled = differenceCents == 0L,
            ) {
                Text("Conclude")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private val RESULT_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)
private val RESULT_POSITIVE_COLOR = Color(0xFF2E7D32)

@Composable
private fun SessionResultDialog(
    result: SessionResult,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.primaryContainer,
                            0.46f to MaterialTheme.colorScheme.surface,
                        )
                    )
                    .padding(top = 24.dp, bottom = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val context = LocalContext.current
                    val appIconPainter = remember {
                        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)!!
                        BitmapPainter(drawable.toBitmap(width = 156, height = 156).asImageBitmap())
                    }
                    Image(
                        painter = appIconPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "Session concluded",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = "${result.outcomes.size} players · ${result.sessionDate.format(RESULT_DATE_FORMATTER)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    result.outcomes.forEachIndexed { index, outcome ->
                        if (index > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                                Spacer(modifier = Modifier.size(10.dp))
                                Text(
                                    text = outcome.playerName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Text(
                                text = formatCents(outcome.deltaCents),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    outcome.deltaCents > 0 -> RESULT_POSITIVE_COLOR
                                    outcome.deltaCents < 0 -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }

                if (result.newRecords.isNotEmpty() || result.streakUpdates.isNotEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        SessionHighlights(result)
                    }
                }

                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

private const val DEFAULT_BUY_IN_CENTS = 400L

@Composable
private fun PlayerSelectionDialog(
    players: List<PlayerWithSessionCount>,
    onConfirm: (Set<String>, Set<String>, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPlayerIds by remember { mutableStateOf(emptySet<String>()) }
    var newPlayerNames by remember { mutableStateOf(emptySet<String>()) }
    var buyInText by remember { mutableStateOf(formatCents(DEFAULT_BUY_IN_CENTS)) }
    var showAllPlayers by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    val buyInCents = parseCentsInput(buyInText)

    val frequentPlayers = players.filter { it.sessionsPlayed > 2 }
    val infrequentPlayers = players.filter { it.sessionsPlayed <= 2 }
    val visiblePlayers = if (showAllPlayers) {
        players
    } else {
        frequentPlayers + infrequentPlayers.filter { it.player.id in selectedPlayerIds }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select players") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        visiblePlayers.forEach { playerWithCount ->
                            val player = playerWithCount.player
                            val selected = player.id in selectedPlayerIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedPlayerIds = if (selected) {
                                        selectedPlayerIds - player.id
                                    } else {
                                        selectedPlayerIds + player.id
                                    }
                                },
                                label = { Text(player.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                        newPlayerNames.forEach { name ->
                            FilterChip(
                                selected = true,
                                onClick = { newPlayerNames = newPlayerNames - name },
                                label = { Text(name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showAddPlayerDialog = true },
                            label = { Text("Add player") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                    if (infrequentPlayers.isNotEmpty()) {
                        TextButton(
                            onClick = { showAllPlayers = !showAllPlayers },
                            contentPadding = PaddingValues(vertical = 0.dp, horizontal = 8.dp),
                        ) {
                            Text(if (showAllPlayers) "Show less" else "Show more")
                            Icon(
                                imageVector = if (showAllPlayers) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = buyInText,
                    onValueChange = { buyInText = it },
                    label = { Text("Buy-in") },
                    isError = buyInCents == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { buyInCents?.let { onConfirm(selectedPlayerIds, newPlayerNames, it) } },
                enabled = buyInCents != null && (selectedPlayerIds.isNotEmpty() || newPlayerNames.isNotEmpty()),
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onConfirm = { name ->
                newPlayerNames = newPlayerNames + name
                showAddPlayerDialog = false
            },
            onDismiss = { showAddPlayerDialog = false },
        )
    }
}

@Composable
private fun AddPlayerDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add player") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Player name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
