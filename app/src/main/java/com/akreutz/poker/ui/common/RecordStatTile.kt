package com.akreutz.poker.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val RECORD_POSITIVE_COLOR = Color(0xFF2E7D32)
val RECORD_POSITIVE_CONTAINER_COLOR = Color(0xFFDCEDC8)
val RECORD_POSITIVE_BADGE_COLOR = Color(0xFFC5E1A5)

enum class RecordTone { POSITIVE, NEGATIVE, NEUTRAL, GOLD }

data class RecordTile(
    val label: String,
    val value: String,
    val dateText: String?,
    val icon: ImageVector,
    val tone: RecordTone,
    val playerName: String? = null,
)

@Composable
fun RecordStatTile(tile: RecordTile, modifier: Modifier = Modifier) {
    val accentColor = when (tile.tone) {
        RecordTone.POSITIVE -> RECORD_POSITIVE_COLOR
        RecordTone.NEGATIVE -> MaterialTheme.colorScheme.error
        RecordTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        RecordTone.GOLD -> MaterialTheme.colorScheme.tertiary
    }
    val containerColor = when (tile.tone) {
        RecordTone.POSITIVE -> RECORD_POSITIVE_CONTAINER_COLOR
        RecordTone.NEGATIVE -> MaterialTheme.colorScheme.errorContainer
        RecordTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        RecordTone.GOLD -> MaterialTheme.colorScheme.tertiaryContainer
    }

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            if (tile.playerName != null) {
                Text(
                    text = tile.playerName,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = tile.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            if (tile.dateText != null) {
                Text(
                    text = tile.dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
