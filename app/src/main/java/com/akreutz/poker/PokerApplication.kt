package com.akreutz.poker

import android.app.Application
import com.akreutz.poker.data.csv.CsvSeedImporter
import com.akreutz.poker.data.local.AppDatabase
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PokerApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        PokerRepository(database.playerDao(), database.sessionDao(), database.sessionEntryDao())
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (repository.isEmpty()) {
                CsvSeedImporter(this@PokerApplication, database).seedIfNeeded()
            }
        }
    }
}
