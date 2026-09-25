package com.akreutz.poker.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.data.model.PlayerSkillSummary
import com.akreutz.poker.data.model.SkillLuckAnalysis
import com.akreutz.poker.ui.SimpleViewModelFactory
import com.akreutz.poker.ui.common.formatCents
import com.akreutz.poker.ui.common.formatSessions
import kotlin.math.roundToInt

@Composable
fun AnalysisScreen() {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: AnalysisViewModel = viewModel(
        factory = SimpleViewModelFactory { AnalysisViewModel(application.repository) },
    )
    val uiState by viewModel.uiState.collectAsState()
    val analysis = uiState.analysis

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MinSessionsControl(
                minSessions = uiState.minSessionsPerPlayer,
                onMinSessionsChange = viewModel::setMinSessionsPerPlayer,
            )
        }
        if (analysis == null) {
            item {
                Text(
                    text = if (uiState.isLoading) {
                        ""
                    } else {
                        "Not enough session history yet. Need at least " +
                            "${uiState.minSessionsPerPlayer} sessions each for " +
                            "${SkillLuckAnalysis.MIN_ELIGIBLE_PLAYERS}+ players to see a skill/luck " +
                            "breakdown - try lowering the minimum above."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                )
            }
        } else {
            item { SkillLuckSummaryCard(analysis) }
            item { SkillLuckMethodologyNote(analysis.minSessionsPerPlayer) }
            item {
                Text(
                    text = "Per-player results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val eligibleSummaries = analysis.playerSummaries.filter { it.isEligible }
            // Shared x-axis range across every player's bell curve, so their widths/positions are
            // directly comparable at a glance instead of each being independently auto-scaled.
            val edgeRange = eligibleSummaries.mapNotNull { it.shrunkEdgeCredibleIntervalCents }.let { intervals ->
                if (intervals.isEmpty()) {
                    null
                } else {
                    val low = intervals.minOf { it.start }
                    val high = intervals.maxOf { it.endInclusive }
                    val padding = (high - low) * 0.15
                    (low - padding)..(high + padding)
                }
            }
            items(eligibleSummaries) { summary ->
                PlayerSkillRow(summary, edgeRange)
            }
            item { PlayerResultsMethodologyNote() }
        }
    }
}

@Composable
private fun MinSessionsControl(minSessions: Int, onMinSessionsChange: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(text = "Min sessions per player", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                IconButton(
                    onClick = { onMinSessionsChange((minSessions - 1).coerceAtLeast(1)) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease minimum sessions")
                }
                Text(
                    text = minSessions.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center,
                )
                IconButton(
                    onClick = { onMinSessionsChange(minSessions + 1) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase minimum sessions")
                }
            }
        }
    }
}

@Composable
private fun SkillLuckSummaryCard(analysis: SkillLuckAnalysis) {
    val skillPercent = (analysis.skillProportion * 100).roundToInt()
    val luckPercent = 100 - skillPercent

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Skill vs. luck", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Based on ${analysis.totalSessions} sessions across ${analysis.eligiblePlayerCount} players",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))

            SkillLuckBar(
                skillPercent = skillPercent,
                confidenceInterval = analysis.skillProportionConfidenceInterval,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "$skillPercent% skill", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Persistent differences between players", style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(text = "$luckPercent% luck", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Session-to-session variance", style = MaterialTheme.typography.bodySmall)
                }
            }

            val confidenceInterval = analysis.skillProportionConfidenceInterval
            if (confidenceInterval != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val lowPercent = (confidenceInterval.start * 100).roundToInt()
                val highPercent = (confidenceInterval.endInclusive * 100).roundToInt()
                Text(
                    text = "90% confidence interval: $lowPercent%–$highPercent% skill",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val SKILL_LUCK_BAR_HEIGHT = 20.dp

@Composable
private fun SkillLuckBar(
    skillPercent: Int,
    confidenceInterval: ClosedFloatingPointRange<Double>?,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(SKILL_LUCK_BAR_HEIGHT),
    ) {
        val barWidth = maxWidth

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(skillPercent.coerceIn(0, 100).toFloat().coerceAtLeast(0.01f))
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Box(
                modifier = Modifier
                    .weight((100 - skillPercent).coerceIn(0, 100).toFloat().coerceAtLeast(0.01f))
                    .fillMaxSize()
                    .background(Color.White),
            )
        }

        if (confidenceInterval != null) {
            val lowFraction = confidenceInterval.start.coerceIn(0.0, 1.0).toFloat()
            val highFraction = confidenceInterval.endInclusive.coerceIn(0.0, 1.0).toFloat()

            // A translucent band spanning the CI range, in the skill bar's own color.
            Box(
                modifier = Modifier
                    .offset(x = barWidth * lowFraction)
                    .width(barWidth * (highFraction - lowFraction))
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun SkillLuckMethodologyNote(minSessionsPerPlayer: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Estimated with a one-way ANOVA / intraclass-correlation decomposition: the skill " +
                "share is the fraction of result variance explained by persistent differences between " +
                "players, after removing the noise expected from session-to-session luck alone. Players " +
                "with fewer than $minSessionsPerPlayer sessions are excluded from the calculation " +
                "(their own results are too noisy to tell skill from luck) and from the list below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun PlayerResultsMethodologyNote() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "\"Edge\" is a 90% Bayesian credible interval combining a player's own results with " +
                "how much real skill variation exists across the whole group. The curve under each " +
                "player is the full shape of that belief, with the shaded area indicating the credible " +
                "interval - the wider or flatter the curve, the less the data can yet tell skill from " +
                "luck for that player.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Composable
private fun PlayerSkillRow(summary: PlayerSkillSummary, sharedEdgeRangeCents: ClosedFloatingPointRange<Double>?) {
    val meanCents = summary.meanDeltaCents.roundToInt().toLong()
    val meanColor = when {
        summary.meanDeltaCents > 0 -> MaterialTheme.colorScheme.primary
        summary.meanDeltaCents < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val credibleInterval = summary.shrunkEdgeCredibleIntervalCents
    val edgeText = if (credibleInterval != null) {
        val lowCents = credibleInterval.start.roundToInt().toLong()
        val highCents = credibleInterval.endInclusive.roundToInt().toLong()
        "Edge ${formatCents(lowCents)} to ${formatCents(highCents)}"
    } else {
        "Edge ${formatCents(meanCents)}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${summary.playerName} · ${formatSessions(summary.sessionsPlayed)}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = edgeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = meanColor,
                    fontWeight = if (summary.isStatisticallySignificant) FontWeight.Bold else FontWeight.Normal,
                )
            }
            val stdDev = summary.posteriorStdDevCents
            if (stdDev != null && sharedEdgeRangeCents != null) {
                EdgeDistributionCurve(
                    meanCents = summary.shrunkEdgeCents ?: summary.meanDeltaCents,
                    stdDevCents = stdDev,
                    credibleIntervalCents = credibleInterval,
                    rangeCents = sharedEdgeRangeCents,
                    curveColor = meanColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                )
            }
        }
    }
}

/**
 * Renders this player's posterior edge distribution as a bell curve (Normal(meanCents, stdDevCents^2))
 * over [rangeCents], with the 90% credible interval shaded underneath and a zero-edge reference line -
 * a compact visual complement to the numeric edge/range shown above it.
 */
@Composable
private fun EdgeDistributionCurve(
    meanCents: Double,
    stdDevCents: Double,
    credibleIntervalCents: ClosedFloatingPointRange<Double>?,
    rangeCents: ClosedFloatingPointRange<Double>,
    curveColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = curveColor.copy(alpha = 0.2f)
    val labelPaint = remember(gridColor) {
        android.graphics.Paint().apply {
            color = gridColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Canvas(modifier = modifier) {
        // Reserve a strip at the top for the "0€" label above the zero line; the curve itself is
        // drawn in the remaining height below it.
        val labelHeight = 14.dp.toPx()
        val curveTop = labelHeight
        val curveHeight = size.height - labelHeight

        val rangeWidth = (rangeCents.endInclusive - rangeCents.start).takeIf { it > 0 } ?: return@Canvas
        fun xForCents(cents: Double): Float =
            (((cents - rangeCents.start) / rangeWidth) * size.width).toFloat()

        // Zero-edge reference line: the "this could just be luck" baseline.
        val zeroX = xForCents(0.0)
        if (zeroX in 0f..size.width) {
            drawLine(
                color = gridColor.copy(alpha = 0.3f),
                start = Offset(zeroX, curveTop),
                end = Offset(zeroX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            labelPaint.textSize = labelHeight * 0.85f
            drawContext.canvas.nativeCanvas.drawText(
                "0€",
                zeroX,
                labelHeight * 0.8f,
                labelPaint,
            )
        }

        // Normal PDF, sampled across the shared range and normalized against its peak at the mean
        // (density there is always 1 before normalization), so the curve's height is correct even
        // if the mean falls outside the shared range.
        fun densityAt(cents: Double): Double {
            val z = (cents - meanCents) / stdDevCents
            return kotlin.math.exp(-0.5 * z * z)
        }
        fun yForDensity(density: Double): Float = (curveTop + curveHeight * (1.0 - density)).toFloat()
        fun pointForCents(cents: Double): Offset = Offset(xForCents(cents), yForDensity(densityAt(cents)))

        val sampleCount = 64
        val curvePoints = (0..sampleCount).map { i ->
            pointForCents(rangeCents.start + rangeWidth * i / sampleCount)
        }

        // Shade the area under the curve within the 90% credible interval. The boundary points are
        // computed exactly at lowX/highX (rather than snapped to the nearest sampled curve point),
        // so the shaded region's left/right edges are vertical instead of angled.
        if (credibleIntervalCents != null) {
            val lowCents = credibleIntervalCents.start.coerceIn(rangeCents.start, rangeCents.endInclusive)
            val highCents = credibleIntervalCents.endInclusive.coerceIn(rangeCents.start, rangeCents.endInclusive)
            val lowPoint = pointForCents(lowCents)
            val highPoint = pointForCents(highCents)
            val shadedPath = Path().apply {
                moveTo(lowPoint.x, size.height)
                lineTo(lowPoint.x, lowPoint.y)
                curvePoints.filter { it.x in lowPoint.x..highPoint.x }.forEach { lineTo(it.x, it.y) }
                lineTo(highPoint.x, highPoint.y)
                lineTo(highPoint.x, size.height)
                close()
            }
            drawPath(path = shadedPath, color = fillColor)
        }

        val curvePath = Path().apply {
            moveTo(curvePoints.first().x, curvePoints.first().y)
            curvePoints.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path = curvePath, color = curveColor, style = Stroke(width = 2.dp.toPx()))
    }
}
