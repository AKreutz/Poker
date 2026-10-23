package com.akreutz.poker.data.watch

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Talks to the Poker watch app through the Connect IQ Mobile SDK. All SDK callbacks arrive on
 * the main thread, so [scope] should run on the main dispatcher.
 *
 * @param useSimulator connect to the Connect IQ simulator over adb instead of a real watch.
 */
class GarminWatchConnection(
    private val context: Context,
    appId: String,
    useSimulator: Boolean,
    private val bridge: WatchSessionBridge,
    private val scope: CoroutineScope,
) {
    private val connectIQ = ConnectIQ.getInstance(
        context,
        if (useSimulator) ConnectIQ.IQConnectType.TETHERED else ConnectIQ.IQConnectType.WIRELESS,
    )
    private val watchApp = IQApp(appId)
    private val connectedDevices = mutableSetOf<IQDevice>()
    private var latestState: Map<String, Any>? = null
    private var stateJob: Job? = null
    private var sdkReady = false

    fun start() {
        connectIQ.initialize(
            context,
            /* autoUI = */ false,
            object : ConnectIQ.ConnectIQListener {
                override fun onSdkReady() {
                    sdkReady = true
                    registerDevices()
                    stateJob = scope.launch {
                        bridge.state.collect { state ->
                            latestState = state
                            connectedDevices.toList().forEach { send(it, state) }
                        }
                    }
                }

                override fun onInitializeError(errorStatus: ConnectIQ.IQSdkErrorStatus) {
                    Log.w(TAG, "Connect IQ SDK unavailable: $errorStatus")
                }

                override fun onSdkShutDown() {
                    sdkReady = false
                }
            },
        )
    }

    fun stop() {
        stateJob?.cancel()
        connectedDevices.clear()
        if (!sdkReady) return
        runCatching {
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(context)
        }.onFailure { Log.w(TAG, "Connect IQ shutdown failed", it) }
        sdkReady = false
    }

    private fun registerDevices() {
        val devices = runCatching { connectIQ.knownDevices }.getOrElse {
            Log.w(TAG, "Could not list Garmin devices", it)
            return
        }
        devices.forEach { device ->
            runCatching {
                connectIQ.registerForDeviceEvents(device) { d, status -> onDeviceStatus(d, status) }
                connectIQ.registerForAppEvents(device, watchApp) { d, _, messages, status ->
                    if (status == ConnectIQ.IQMessageStatus.SUCCESS) onMessages(d, messages)
                }
                onDeviceStatus(device, connectIQ.getDeviceStatus(device))
            }.onFailure { Log.w(TAG, "Could not register for ${device.friendlyName}", it) }
        }
    }

    private fun onDeviceStatus(device: IQDevice, status: IQDevice.IQDeviceStatus) {
        if (status == IQDevice.IQDeviceStatus.CONNECTED) {
            if (connectedDevices.add(device)) latestState?.let { send(device, it) }
        } else {
            connectedDevices.remove(device)
        }
    }

    private fun onMessages(device: IQDevice, messages: List<Any>?) {
        messages.orEmpty().mapNotNull(WatchProtocol::decodeCommand).forEach { command ->
            scope.launch { bridge.handle(command)?.let { send(device, it) } }
        }
    }

    private fun send(device: IQDevice, message: Map<String, Any>) {
        runCatching {
            connectIQ.sendMessage(device, watchApp, message) { _, _, status ->
                if (status != ConnectIQ.IQMessageStatus.SUCCESS) Log.w(TAG, "Sending to watch failed: $status")
            }
        }.onFailure { Log.w(TAG, "Sending to watch failed", it) }
    }

    private companion object {
        const val TAG = "GarminWatch"
    }
}
