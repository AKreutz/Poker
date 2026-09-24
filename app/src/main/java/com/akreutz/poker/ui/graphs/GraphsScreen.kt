package com.akreutz.poker.ui.graphs

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.poker.ui.SimpleViewModelFactory
import com.akreutz.poker.PokerApplication
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val ALL_PLAYERS_OPTION = "All players"
private const val MIN_SESSIONS_DEFAULT = 5
private const val ALL_PLAYERS_ID = "__all_players__"

private val X_AXIS_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM")

/** Fixed palette assigned to plotted lines in order, so the legend can reuse the same colors. */
private val LINE_COLORS = listOf(
    Color(0xFF4285F4),
    Color(0xFF34A853),
    Color(0xFFFBBC04),
    Color(0xFFEA4335),
    Color(0xFF9C27B0),
    Color(0xFF00ACC1),
)

private fun bottomAxisValueFormatter(axisDates: List<LocalDate?>) = CartesianValueFormatter { _, value, _ ->
    axisDates.getOrNull(value.toInt())?.format(X_AXIS_DATE_FORMATTER) ?: "Start"
}

@Composable
fun GraphsScreen(onStateChanged: (GraphsFullScreenState) -> Unit = {}) {
    val application = LocalContext.current.applicationContext as PokerApplication
    val viewModel: GraphsViewModel = viewModel(
        factory = SimpleViewModelFactory { GraphsViewModel(application.repository) },
    )
    val players by viewModel.players.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val balanceSeriesByPlayerId = uiState.balanceSeriesByPlayerId

    var selectedPlayerId by remember { mutableStateOf(ALL_PLAYERS_ID) }
    var menuExpanded by remember { mutableStateOf(false) }
    var minSessions by remember { mutableIntStateOf(MIN_SESSIONS_DEFAULT) }
    val filteredPlayers = players.filter { it.sessionsPlayed >= minSessions }
    val selectedOption = if (selectedPlayerId == ALL_PLAYERS_ID) {
        ALL_PLAYERS_OPTION
    } else {
        players.firstOrNull { it.player.id == selectedPlayerId }?.player?.name ?: ALL_PLAYERS_OPTION
    }

    val seriesToPlot = if (selectedPlayerId == ALL_PLAYERS_ID) {
        filteredPlayers.mapNotNull { balanceSeriesByPlayerId[it.player.id] }
    } else {
        listOfNotNull(balanceSeriesByPlayerId[selectedPlayerId])
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(seriesToPlot) {
        if (seriesToPlot.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                seriesToPlot.forEach { playerSeries ->
                    series(
                        x = playerSeries.points.map { it.dateIndex },
                        y = playerSeries.points.map { it.cumulativeBalanceCents / 100f },
                    )
                }
            }
        }
    }

    LaunchedEffect(seriesToPlot, uiState.axisDates, modelProducer) {
        onStateChanged(
            GraphsFullScreenState(
                seriesToPlot = seriesToPlot,
                axisDates = uiState.axisDates,
                modelProducer = modelProducer,
            ),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Box {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(selectedOption)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(ALL_PLAYERS_OPTION) },
                            onClick = {
                                selectedPlayerId = ALL_PLAYERS_ID
                                menuExpanded = false
                            },
                        )
                        filteredPlayers.forEach { playerWithCount ->
                            DropdownMenuItem(
                                text = { Text(playerWithCount.player.name) },
                                onClick = {
                                    selectedPlayerId = playerWithCount.player.id
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Min sessions played", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    IconButton(
                        onClick = { minSessions = (minSessions - 1).coerceAtLeast(0) },
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
                        onClick = { minSessions += 1 },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase minimum sessions")
                    }
                }
            }
        }
        GraphLegend(seriesToPlot = seriesToPlot, modifier = Modifier.padding(top = 12.dp))
        GraphChart(
            modelProducer = modelProducer,
            axisDates = uiState.axisDates,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun GraphLegend(seriesToPlot: List<PlayerBalanceSeries>, modifier: Modifier = Modifier) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        seriesToPlot.forEachIndexed { index, playerSeries ->
            val color = LINE_COLORS[index % LINE_COLORS.size]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = playerSeries.playerName, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GraphChart(
    modelProducer: CartesianChartModelProducer,
    axisDates: List<LocalDate?>,
    modifier: Modifier = Modifier,
) {
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LINE_COLORS.map { color ->
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill = Fill(color.toArgb())),
                        )
                    },
                ),
                rangeProvider = CartesianLayerRangeProvider.fixed(minX = 0.0),
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = bottomAxisValueFormatter(axisDates),
            ),
            layerPadding = { CartesianLayerPadding(scalableStartDp = 0f, unscalableStartDp = 0f) },
        ),
        modelProducer = modelProducer,
        zoomState = rememberVicoZoomState(initialZoom = Zoom.Content, minZoom = Zoom.Content),
        modifier = modifier,
    )
}

/** Snapshot of a [GraphsScreen]'s chart data needed to render it full screen. */
data class GraphsFullScreenState(
    val seriesToPlot: List<PlayerBalanceSeries>,
    val axisDates: List<LocalDate?>,
    val modelProducer: CartesianChartModelProducer,
)

@Composable
fun FullScreenGraphOverlay(state: GraphsFullScreenState, onDismiss: () -> Unit) {
    val activity = LocalActivity.current

    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                GraphLegend(seriesToPlot = state.seriesToPlot)
                GraphChart(
                    modelProducer = state.modelProducer,
                    axisDates = state.axisDates,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit full screen")
            }
        }
    }
}
