package com.akreutz.poker

import android.app.Application
import android.util.Log
import com.akreutz.poker.data.local.AppDatabase
import com.akreutz.poker.data.repository.PokerRepository
import com.akreutz.poker.data.sync.LocalChangeTracker
import com.akreutz.poker.data.sync.SyncManager
import com.akreutz.poker.data.sync.auth.GoogleAuthManager
import com.akreutz.poker.data.sync.drive.GoogleDriveDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class Failed(val message: String?) : SyncState
}

class PokerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val database by lazy { AppDatabase.getInstance(this) }

    val localChangeTracker = LocalChangeTracker()

    val repository by lazy {
        PokerRepository(
            database.playerDao(),
            database.sessionDao(),
            database.sessionEntryDao(),
            database.purgedIdDao(),
            localChangeTracker,
        )
    }

    val authManager by lazy { GoogleAuthManager(this) }

    // Swapping to Firestore/Supabase/etc. later means replacing GoogleDriveDataSource only.
    val syncManager by lazy {
        SyncManager(
            database.playerDao(),
            database.sessionDao(),
            database.sessionEntryDao(),
            database.purgedIdDao(),
            GoogleDriveDataSource(authManager),
        )
    }

    /** Called once MainActivity has completed sign-in, so a Drive-backed sync can proceed. */
    fun syncInBackground() {
        applicationScope.launch {
            runSync()
        }
    }

    /** Triggered from the UI's sync button. Signs in first if that hasn't happened yet. */
    fun syncNow() {
        applicationScope.launch {
            if (authManager.signedInAccount == null) {
                authManager.signIn()
            }
            runSync()
        }
    }

    private suspend fun runSync() {
        _syncState.value = SyncState.Syncing
        _syncState.value = runCatching { syncManager.sync() }
            .fold(
                onSuccess = {
                    localChangeTracker.markSynced()
                    SyncState.Success
                },
                onFailure = {
                    Log.e("PokerSync", "sync() failed", it)
                    SyncState.Failed(it.message)
                },
            )
    }
}
