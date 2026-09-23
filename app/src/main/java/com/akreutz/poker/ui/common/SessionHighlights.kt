package com.akreutz.poker.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akreutz.poker.data.model.SessionResult
import com.akreutz.poker.data.model.StreakUpdate

@Composable
fun SessionHighlights(result: SessionResult) {
    if (result.newRecords.isEmpty() && result.streakUpdates.isEmpty()) {
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        result.newRecords.forEach { record ->
            HighlightTile(
                icon = Icons.Filled.EmojiEvents,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                title = "New record",
                subtitle = "${record.playerName} — ${record.label}",
            )
        }
        result.streakUpdates.forEach { update ->
            StreakUpdateTile(update)
        }
    }
}

@Composable
private fun StreakUpdateTile(update: StreakUpdate) {
    val kind = if (update.isWinStreak) "win" else "loss"
    when (update) {
        is StreakUpdate.Continued -> HighlightTile(
            icon = Icons.Filled.LocalFireDepartment,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
            title = "Streak continues",
            subtitle = "${update.playerName} extended their $kind streak to ${formatSessions(update.length)}",
        )

        is StreakUpdate.Broken -> HighlightTile(
            icon = Icons.Filled.LinkOff,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            iconColor = MaterialTheme.colorScheme.onErrorContainer,
            title = "Streak broken",
            subtitle = "${update.playerName}'s ${update.length}-session $kind streak ended",
        )

        is StreakUpdate.Stood -> HighlightTile(
            icon = Icons.Filled.HourglassEmpty,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            title = "Streak stands",
            subtitle = "${update.playerName}'s ${update.length}-session $kind streak sat this one out",
        )

        is StreakUpdate.TakenOver -> HighlightTile(
            icon = Icons.Filled.SwapHoriz,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            title = "New streak leader",
            subtitle = "${update.playerName} took over the $kind streak lead with ${update.length}, past " +
                "${update.previousPlayerName}'s ${update.previousLength}",
        )
    }
}

@Composable
fun HighlightTile(
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = iconColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
