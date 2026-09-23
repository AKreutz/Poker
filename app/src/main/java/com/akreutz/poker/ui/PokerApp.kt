package com.akreutz.poker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.SyncState
import com.akreutz.poker.navigation.PokerDestination
import com.akreutz.poker.navigation.PokerRoutes
import com.akreutz.poker.ui.graphs.FullScreenGraphOverlay
import com.akreutz.poker.ui.graphs.GraphsFullScreenState
import com.akreutz.poker.ui.graphs.GraphsScreen
import com.akreutz.poker.ui.home.OverviewScreen
import com.akreutz.poker.ui.sessions.SessionsScreen
import com.akreutz.poker.ui.stats.PlayerDetailScreen
import com.akreutz.poker.ui.stats.StatsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isPlayerDetail = currentDestination?.route == PokerRoutes.PLAYER_DETAIL
    val currentTab = if (isPlayerDetail) {
        PokerDestination.Stats
    } else {
        PokerDestination.entries.firstOrNull { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        } ?: PokerDestination.Overview
    }

    val application = LocalContext.current.applicationContext as PokerApplication
    val syncState by application.syncState.collectAsState()
    val hasUnsyncedChanges by application.localChangeTracker.hasUnsyncedChanges.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(syncState) {
        when (val state = syncState) {
            is SyncState.Success -> snackbarHostState.showSnackbar("Sync complete")
            is SyncState.Failed -> snackbarHostState.showSnackbar(
                state.message?.let { "Sync failed: $it" } ?: "Sync failed"
            )
            else -> Unit
        }
    }

    var graphsState by remember { mutableStateOf<GraphsFullScreenState?>(null) }
    var fullScreenGraphState by remember { mutableStateOf<GraphsFullScreenState?>(null) }
    val isGraphsTab = currentTab == PokerDestination.Graphs && !isPlayerDetail

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {
                val state = graphsState
                if (isGraphsTab && state != null) {
                    FloatingActionButton(onClick = { fullScreenGraphState = state }) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text(if (isPlayerDetail) "Player Stats" else currentTab.label) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    navigationIcon = {
                        if (isPlayerDetail) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { application.syncNow() },
                            enabled = syncState != SyncState.Syncing,
                        ) {
                            if (syncState == SyncState.Syncing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                BadgedBox(
                                    badge = { if (hasUnsyncedChanges) Badge() },
                                ) {
                                    Icon(Icons.Filled.Sync, contentDescription = "Sync")
                                }
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    PokerDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == currentTab,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                val iconRes = destination.iconRes
                                if (iconRes != null) {
                                    Icon(painterResource(iconRes), contentDescription = destination.label)
                                } else {
                                    Icon(destination.icon!!, contentDescription = destination.label)
                                }
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = PokerDestination.Overview.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(PokerDestination.Overview.route) { OverviewScreen() }
                composable(PokerDestination.Stats.route) {
                    StatsScreen(
                        onPlayerClick = { playerId ->
                            navController.navigate(PokerRoutes.playerDetail(playerId))
                        },
                    )
                }
                composable(PokerDestination.Graphs.route) {
                    GraphsScreen(onStateChanged = { graphsState = it })
                }
                composable(PokerDestination.Sessions.route) { SessionsScreen() }
                composable(
                    route = PokerRoutes.PLAYER_DETAIL,
                    arguments = listOf(navArgument("playerId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val playerId = backStackEntry.arguments?.getString("playerId").orEmpty()
                    PlayerDetailScreen(playerId = playerId)
                }
            }
        }

        fullScreenGraphState?.let { state ->
            FullScreenGraphOverlay(
                state = state,
                onDismiss = { fullScreenGraphState = null },
            )
        }
    }
}
