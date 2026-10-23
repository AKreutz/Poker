package com.akreutz.poker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.akreutz.poker.data.watch.WatchSyncService
import com.akreutz.poker.ui.PokerApp
import com.akreutz.poker.ui.theme.PokerTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingConsentResult: CompletableDeferred<Boolean>? = null

    private val consentResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pendingConsentResult?.complete(result.resultCode == Activity.RESULT_OK)
        pendingConsentResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokerTheme {
                PokerApp()
            }
        }

        val app = application as PokerApplication
        app.authManager.consentLauncher = { intent, result ->
            pendingConsentResult = result
            consentResultLauncher.launch(intent)
        }
        lifecycleScope.launch {
            if (app.authManager.signedInAccount == null) {
                app.authManager.signIn()
            }
            app.syncInBackground()
        }

        if (BuildConfig.GARMIN_APP_ID.isNotEmpty()) {
            // Started from the foreground (Android forbids it from the background); the service
            // stops itself when the session ends, and this restarts it for the next session.
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    app.repository.observeOpenSession()
                        .map { it != null }
                        .distinctUntilChanged()
                        .filter { it }
                        .collect { WatchSyncService.start(this@MainActivity) }
                }
            }
        }
    }
}
