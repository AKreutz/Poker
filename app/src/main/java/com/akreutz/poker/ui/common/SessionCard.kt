package com.akreutz.poker.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import com.akreutz.poker.data.local.entity.SessionStatus
import com.akreutz.poker.data.model.SessionEntryWithPlayer
import com.akreutz.poker.data.model.SessionResult
import com.akreutz.poker.data.model.SessionWithEntries
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val SESSION_DATE_FORMATTER =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY)

@Composable
fun SessionCard(
    sessionWithEntries: SessionWithEntries,
    sessionResult: SessionResult? = null,
    initiallyExpanded: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onRemoveEntry: ((SessionEntryEntity) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = sessionWithEntries.session.date.format(SESSION_DATE_FORMATTER),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    val subtitle = buildString {
                        sessionWithEntries.handsPlayed?.let { append(formatHands(it)) }
                        sessionDuration(sessionWithEntries)?.let { duration ->
                            if (isNotEmpty()) append(" · ")
                            append(formatDuration(duration))
                        }
                    }
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDelete != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete session")
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                    )
                }
            }

            if (expanded) {
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        SessionEntriesList(sessionWithEntries, onRemoveEntry = onRemoveEntry)
                    }
                    if (sessionResult != null &&
                        (sessionResult.newRecords.isNotEmpty() || sessionResult.streakUpdates.isNotEmpty())
                    ) {
                        SessionHighlights(sessionResult)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete session?") },
            text = { Text("This will permanently remove this session and everything recorded in it.") },
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

@Composable
fun StaticSessionCard(sessionWithEntries: SessionWithEntries) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = sessionWithEntries.session.date.format(SESSION_DATE_FORMATTER),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SessionEntriesList(sessionWithEntries)
        }
    }
}

/**
 * Approximates how long a session lasted from [SessionEntity.createdAt] (session start) to
 * [SessionEntity.updatedAt] (last touched when concluded). Only meaningful once concluded, since
 * `updatedAt` keeps moving forward while the session is still open.
 */
private fun sessionDuration(sessionWithEntries: SessionWithEntries): Duration? {
    val session = sessionWithEntries.session
    if (session.status != SessionStatus.CONCLUDED) return null
    return Duration.between(session.createdAt, session.updatedAt).takeIfMeasurable()
}

private val SESSION_ENTRY_POSITIVE_COLOR = Color(0xFF2E7D32)

@Composable
private fun SessionEntriesList(
    sessionWithEntries: SessionWithEntries,
    onRemoveEntry: ((SessionEntryEntity) -> Unit)? = null,
) {
    var entryPendingRemoval by remember { mutableStateOf<SessionEntryWithPlayer?>(null) }
    val totalHandsPlayed = sessionWithEntries.handsPlayed ?: 0
    val sortedEntries = sessionWithEntries.entries.sortedByDescending { it.entry.deltaCents }
    sortedEntries.forEachIndexed { index, entryWithPlayer ->
        if (index > 0) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        val entry = entryWithPlayer.entry
        val delta = entry.deltaCents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (onRemoveEntry != null) {
                        it.combinedClickable(onClick = {}, onLongClick = { entryPendingRemoval = entryWithPlayer })
                    } else {
                        it
                    }
                }
                .padding(vertical = 10.dp),
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
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entryWithPlayer.player.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        entry.handsWon?.let { handsWon ->
                            Spacer(modifier = Modifier.width(6.dp))
                            val wonText = if (totalHandsPlayed > 0) {
                                val percentage = handsWon * 100 / totalHandsPlayed
                                "${formatHands(handsWon)} won ($percentage%)"
                            } else {
                                "${formatHands(handsWon)} won"
                            }
                            Text(
                                text = wonText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Text(
                        text = "${formatCents(entry.buyInCents)} → ${formatCents(entry.cashOutCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatCents(delta),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    delta > 0 -> SESSION_ENTRY_POSITIVE_COLOR
                    delta < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }

    val target = entryPendingRemoval
    if (target != null && onRemoveEntry != null) {
        AlertDialog(
            onDismissRequest = { entryPendingRemoval = null },
            title = { Text("Remove ${target.player.name}?") },
            text = { Text("This will remove ${target.player.name} and their buy-ins from this session.") },
            confirmButton = {
                Button(onClick = {
                    entryPendingRemoval = null
                    onRemoveEntry(target.entry)
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { entryPendingRemoval = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
