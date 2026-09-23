package com.akreutz.poker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.akreutz.poker.ui.PokerApp
import com.akreutz.poker.ui.theme.PokerTheme
import kotlinx.coroutines.CompletableDeferred
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
    }
}
