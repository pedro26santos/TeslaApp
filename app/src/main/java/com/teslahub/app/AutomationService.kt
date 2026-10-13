package com.teslahub.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock

class AutomationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var monitor: TeslaBluetoothMonitor? = null
    private var session = ConnectionSession()
    private var observation = ConnectionSession.Observation.UNKNOWN
    private var lastRefresh = 0L
    private var lastStartAttempt = 0L
    private var lastHotspotCheck = 0L
    private var needsHotspot = false
    private var notificationText = ""
    private var watchingAddress: String? = null
    private val tick = object : Runnable {
        override fun run() {
            val prefs = HubPreferences(this@AutomationService)
            if (!prefs.automationEnabled) { stopSelf(); return }
            if (!TeslaBluetoothMonitor.hasPermission(this@AutomationService)) {
                HubRuntime.automationLastError = "Nearby Devices permission missing; open connection settings"
                stopSelf()
                return
            }
            val now = SystemClock.elapsedRealtime()
            if (now - lastRefresh >= 5000) { lastRefresh = now; monitor?.refresh() }
            evaluate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("teslahub_connection", "Tesla Hub connection", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationText = ""
        startForeground(3, notification("Starting connection monitor"))
        val prefs = HubPreferences(this).apply { publish() }
        val address = prefs.carAddress
        if (!prefs.automationEnabled || address == null || !TeslaBluetoothMonitor.hasPermission(this)) {
            HubRuntime.automationLastError = "Select the Tesla and grant Nearby Devices permission in connection settings"
            stopSelf()
            return START_NOT_STICKY
        }
        if (monitor == null || watchingAddress != address) {
            monitor?.close()
            NetworkControl.stopAutomatic(this)
            session = ConnectionSession()
            observation = ConnectionSession.Observation.UNKNOWN
            watchingAddress = address
            HubRuntime.automationLastError = null
            HubRuntime.automationMonitorRunning = true
            HubRuntime.automationStatus = "Waiting for selected Tesla"
            monitor = TeslaBluetoothMonitor(this, address) { value, detail ->
                observation = value
                HubRuntime.bluetoothStatus = detail
                evaluate()
            }
            try {
                monitor?.start()
                if (intent?.getBooleanExtra(NetworkControl.CONNECTED_EVENT, false) == true) monitor?.connectedEvent()
            } catch (e: Exception) {
                HubRuntime.automationLastError = "Bluetooth monitor failed: ${e.javaClass.simpleName}"
                stopSelf()
                return START_NOT_STICKY
            }
            handler.removeCallbacks(tick)
            handler.post(tick)
        } else {
            lastStartAttempt = 0L
            if (intent?.getBooleanExtra(NetworkControl.CONNECTED_EVENT, false) == true) monitor?.connectedEvent()
            else monitor?.refresh()
            evaluate()
        }
        // Android may recreate a killed foreground monitor. Saved opt-in and permissions are
        // checked again before observing or starting any networking.
        return START_STICKY
    }

    private fun evaluate() {
        if (!HubRuntime.automationMonitorRunning) return
        val prefs = HubPreferences(this)
        if (!prefs.automationEnabled) { stopSelf(); return }
        val now = SystemClock.elapsedRealtime()
        val grace = prefs.graceSeconds * 1000L
        val effect = session.update(observation, now, grace)
        if (effect == ConnectionSession.Effect.STOP && prefs.stopOnDisconnect) NetworkControl.stopAutomatic(this)
        if (observation == ConnectionSession.Observation.CONNECTED &&
            (effect == ConnectionSession.Effect.START || now - lastStartAttempt >= 15_000)) {
            lastStartAttempt = now
            HubRuntime.automationLastError = NetworkControl.startAutomatic(this)
        }
        HubRuntime.shutdownRemainingSeconds = session.remainingSeconds(now, grace)
        HubRuntime.automationStatus = when {
            HubRuntime.shutdownRemainingSeconds != null -> "Disconnect grace period"
            observation == ConnectionSession.Observation.CONNECTED -> "Selected Tesla connected"
            observation == ConnectionSession.Observation.UNKNOWN -> "Checking Bluetooth state"
            else -> "Waiting for selected Tesla"
        }
        if (observation == ConnectionSession.Observation.CONNECTED && now - lastHotspotCheck >= 5000) {
            lastHotspotCheck = now
            try { needsHotspot = HotspotAssistance.needsActivation(this) }
            catch (e: Exception) { HubRuntime.hotspotStrategy = "Check Personal Hotspot in phone settings"; needsHotspot = true }
        }
        val remaining = HubRuntime.shutdownRemainingSeconds
        val text = when {
            HubRuntime.automationLastError != null -> HubRuntime.automationLastError!!
            remaining != null && prefs.stopOnDisconnect -> "Tesla disconnected; automatic services stop in ${remaining}s"
            remaining != null -> "Tesla disconnected; services will stay running"
            observation == ConnectionSession.Observation.CONNECTED && needsHotspot -> "Tesla connected; enable Personal Hotspot"
            observation == ConnectionSession.Observation.CONNECTED && HubRuntime.localRouteUrl == null -> "Tesla connected; preparing local connection—open Tesla Hub for status"
            observation == ConnectionSession.Observation.CONNECTED -> "Tesla connected; local address ${LocalRoutePolicy.ADDRESS}:8765"
            else -> HubRuntime.automationStatus
        }
        if (text != notificationText) {
            notificationText = text
            getSystemService(NotificationManager::class.java).notify(3, notification(text))
        }
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 31, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val hotspot = PendingIntent.getActivity(this, 32,
            Intent(this, ConnectionSettingsActivity::class.java).putExtra(ConnectionSettingsActivity.OPEN_HOTSPOT, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "teslahub_connection").setContentTitle("Tesla Hub connection")
            .setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true).setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Hotspot settings", hotspot).build()).build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        monitor?.close()
        monitor = null
        HubRuntime.automationMonitorRunning = false
        HubRuntime.shutdownRemainingSeconds = null
        HubRuntime.bluetoothStatus = if (HubPreferences(this).carAddress == null) "No Tesla selected" else "Monitor stopped"
        HubRuntime.automationStatus = if (HubPreferences(this).automationEnabled) "Monitor stopped; reopen Tesla Hub to resume" else "Disabled"
        NetworkControl.stopAutomatic(this)
        super.onDestroy()
    }
}
