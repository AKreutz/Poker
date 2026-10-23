package com.akreutz.poker.data.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.akreutz.poker.BuildConfig
import com.akreutz.poker.MainActivity
import com.akreutz.poker.PokerApplication
import com.akreutz.poker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the watch connection alive while a session is open, so hands can be counted from the
 * watch with the phone's screen off. Stops itself once there is no open session.
 */
class WatchSyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var connection: GarminWatchConnection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as PokerApplication
        startInForeground()

        connection = GarminWatchConnection(
            context = this,
            appId = BuildConfig.GARMIN_APP_ID,
            useSimulator = BuildConfig.GARMIN_USE_SIMULATOR,
            bridge = app.watchSessionBridge,
            scope = scope,
        ).also { it.start() }

        scope.launch {
            app.repository.observeOpenSession().map { it != null }.filter { !it }.first()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        connection?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val channelId = "watch_sync"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.watch_sync_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_playing_cards)
            .setContentTitle(getString(R.string.watch_sync_notification_title))
            .setContentText(getString(R.string.watch_sync_notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0,
        )
    }

    companion object {
        private const val NOTIFICATION_ID = 1

        /** Must be called while the app is in the foreground, or Android refuses the start. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WatchSyncService::class.java))
        }
    }
}
