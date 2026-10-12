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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        server = LocalHttpServer(assets)
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
            HubRuntime.serviceRunning = true
            worker = thread(name = "TeslaHub-network", isDaemon = true) { runServerLoop() }
        }
        return START_NOT_STICKY
    }

    private fun runServerLoop() {
        while (active.get()) {
            try {
                val found = LocalNetwork.discover()
                server.reconcile(found)
                HubRuntime.addresses = server.boundAddresses()
                HubRuntime.serverError = when {
                    found.isEmpty() -> "No private Wi-Fi/hotspot interface detected"
                    HubRuntime.addresses.isEmpty() -> server.lastBindError ?: "Server failed to bind local interface"
                    server.lastBindError != null -> "Running; ${server.lastBindError}"
                    else -> "Running"
                }
                HubRuntime.hotspotStatus = if (found.any {
                    val name = it.interfaceName.lowercase()
                    name.startsWith("ap") || name.startsWith("swlan")
                }) "Possible hotspot interface detected" else "Unknown — check Android hotspot settings"
                if (HubRuntime.addresses.isNotEmpty() && registration == null) registerMdns()
            } catch (e: Exception) {
                HubRuntime.serverError = "Server error: ${e.message ?: e.javaClass.simpleName}"
            }
            try { Thread.sleep(3000) } catch (_: InterruptedException) { break }
        }
    }

    private fun registerMdns() {
        try {
            val manager = getSystemService(NSD_SERVICE) as NsdManager
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    HubRuntime.mdnsStatus = "Service advertised as ${serviceInfo.serviceName}; hostname unverified"
                }
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    HubRuntime.mdnsStatus = "Unavailable (code $errorCode)"
                    registration = null
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
        HubRuntime.serviceRunning = false
        HubRuntime.addresses = emptyList()
        HubRuntime.serverError = "Stopped"
        HubRuntime.mdnsStatus = "Stopped"
        HubRuntime.hotspotStatus = "Unknown"
        super.onDestroy()
    }
}
