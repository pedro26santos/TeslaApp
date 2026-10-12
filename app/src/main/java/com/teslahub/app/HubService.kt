package com.teslahub.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class HubService : Service() {
    private val active = AtomicBoolean(false)
    private lateinit var server: LocalHttpServer
    private var worker: Thread? = null
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    private val stateLock = Any()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        server = LocalHttpServer({ path -> assets.open(path).use { it.readBytes() } })
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("teslahub_server", "Tesla Hub local server", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = Notification.Builder(this, "teslahub_server")
            .setContentTitle("Tesla Hub")
            .setContentText("Local Tesla browser server is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
        if (active.compareAndSet(false, true)) {
            HubRuntime.resetRequests()
            HubRuntime.serviceRunning = true
            worker = thread(name = "TeslaHub-network", isDaemon = true) { runServerLoop() }
        }
        return START_NOT_STICKY
    }

    private fun runServerLoop() {
        while (active.get()) {
            try {
                val networkInfo = NetworkDiagnostics.read(this)
                val found = LocalNetwork.discover(networkInfo.wifiInterfaces)
                server.reconcile(found)
                val bound = server.boundAddresses()
                val checks = if (bound != HubRuntime.serverChecks.map { it.binding } ||
                    System.currentTimeMillis() - HubRuntime.serverCheckedAt >= 15_000) {
                    bound.map { ServerHealthCheck.check(it) }
                } else null
                synchronized(stateLock) {
                    if (!active.get()) return
                    HubRuntime.networkInfo = networkInfo
                    HubRuntime.addresses = bound
                    if (checks != null) {
                        HubRuntime.serverChecks = checks
                        HubRuntime.serverCheckedAt = System.currentTimeMillis()
                    }
                    HubRuntime.serverError = when {
                        found.isEmpty() -> "No private Wi-Fi/hotspot interface detected"
                        bound.isEmpty() -> server.lastBindError ?: "Server failed to bind local interface"
                        server.lastBindError != null -> "Running; ${server.lastBindError}"
                        HubRuntime.serverChecks.none { it.passed } -> "Listening; phone HTTP check failed"
                        else -> "Running"
                    }
                    HubRuntime.hotspotStatus = when {
                        found.isEmpty() -> "No Wi-Fi/hotspot interface found"
                        found.all { it.connectedWifi } -> "Only the phone's connected Wi-Fi is visible; check hotspot address"
                        else -> "Local interface found (${found.filter { !it.connectedWifi }.joinToString { it.interfaceName }}); confirm hotspot in Settings"
                    }
                    if (bound.isNotEmpty() && registration == null) registerMdns()
                }
            } catch (e: Exception) {
                synchronized(stateLock) {
                    if (active.get()) HubRuntime.serverError = "Server error: ${e.message ?: e.javaClass.simpleName}"
                }
            }
            try { Thread.sleep(3000) } catch (_: InterruptedException) { break }
        }
    }

    private fun registerMdns() {
        try {
            val manager = getSystemService(NSD_SERVICE) as NsdManager
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    synchronized(stateLock) {
                        if (active.get()) HubRuntime.mdnsStatus = "Service advertised as ${serviceInfo.serviceName}; hostname unverified"
                    }
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    synchronized(stateLock) {
                        if (active.get()) HubRuntime.mdnsStatus = "Unavailable (code $errorCode)"
                        registration = null
                    }
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    HubRuntime.mdnsStatus = "Stopped"
                }
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    HubRuntime.mdnsStatus = "Unregister failed (code $errorCode)"
                }
            }
            registration = listener
            nsd = manager
            HubRuntime.mdnsStatus = "Registering"
            manager.registerService(NsdServiceInfo().apply {
                serviceName = "teslahub"
                serviceType = "_http._tcp."
                port = 8765
            }, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            registration = null
            HubRuntime.mdnsStatus = "Unavailable: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    override fun onDestroy() {
        active.set(false)
        worker?.interrupt()
        registration?.let { listener ->
            try { nsd?.unregisterService(listener) } catch (_: Exception) { }
        }
        registration = null
        server.close()
        synchronized(stateLock) {
            HubRuntime.serviceRunning = false
            HubRuntime.addresses = emptyList()
            HubRuntime.serverChecks = emptyList()
            HubRuntime.serverCheckedAt = 0L
            HubRuntime.serverError = "Stopped"
            HubRuntime.mdnsStatus = "Stopped"
            HubRuntime.hotspotStatus = "Unknown"
            HubRuntime.networkInfo = NetworkDiagnostics.Snapshot()
            HubRuntime.resetRequests()
        }
        super.onDestroy()
    }
}
