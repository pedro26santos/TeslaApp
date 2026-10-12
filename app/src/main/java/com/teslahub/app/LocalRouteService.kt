package com.teslahub.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/** Manual routing feasibility test. No remote gateway, default route, DNS override, or media capture. */
class LocalRouteService : VpnService() {
    private val active = AtomicBoolean(false)
    private val stateLock = Any()
    private var tunnel: ParcelFileDescriptor? = null
    private var server: LocalHttpServer? = null
    private var worker: Thread? = null
    private val packets = AtomicLong()

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("teslahub_route", "Tesla Hub local route test", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, Notification.Builder(this, "teslahub_route")
            .setContentTitle("Tesla Hub local route test")
            .setContentText("Experimental local address; no remote VPN server")
            .setSmallIcon(android.R.drawable.ic_dialog_info).setOngoing(true).build())
        if (active.compareAndSet(false, true)) {
            packets.set(0)
            HubRuntime.localRoutePackets = 0L
            HubRuntime.localRouteLastPacket = "None"
            HubRuntime.localRouteMonitorError = null
            HubRuntime.localRouteStatus = "Starting local address test"
            worker = thread(name = "TeslaHub-local-route", isDaemon = true) { runRoute() }
        }
        return START_NOT_STICKY
    }

    private fun runRoute() {
        try {
            check(prepare(this) == null) { "Approve Android's VPN permission in Tesla Hub first" }
            check(NetworkInterface.getByInetAddress(LocalRoutePolicy.binding.address) == null) {
                "The local test address is already in use on another interface"
            }
            val descriptor = Builder()
                .setSession("Tesla Hub local address test")
                .addAddress(LocalRoutePolicy.ADDRESS, 32)
                .addRoute(LocalRoutePolicy.ADDRESS, 32)
                .addAllowedApplication(packageName)
                .allowFamily(OsConstants.AF_INET6)
                .setMtu(1500).setBlocking(false)
                .setConfigureIntent(android.app.PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE))
                .establish() ?: error("Android did not create the local VPN interface")
            synchronized(stateLock) {
                if (!active.get()) { descriptor.close(); return }
                tunnel = descriptor
            }
            // The server uses kernel TCP delivery to an assigned link-local address.
            // TUN can still emit IPv6 interface control packets; count and classify them,
            // without treating them as proof of IPv4 HTTP traffic or storing payloads.
            thread(name = "TeslaHub-route-packets", isDaemon = true) {
                try {
                    val pollFd = StructPollfd().apply {
                        fd = descriptor.fileDescriptor
                        events = OsConstants.POLLIN.toShort()
                    }
                    val buffer = ByteArray(1600)
                    while (isCurrent(descriptor)) {
                        try {
                            if (Os.poll(arrayOf(pollFd), 1000) == 0) continue
                            if (!isCurrent(descriptor)) break
                            check((pollFd.revents.toInt() and
                                (OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL)) == 0) {
                                "TUN interface became unavailable"
                            }
                            val count = Os.read(pollFd.fd, buffer, 0, buffer.size)
                            check(count > 0) { "TUN closed unexpectedly" }
                            HubRuntime.localRoutePackets = packets.incrementAndGet()
                            HubRuntime.localRouteLastPacket = LocalRoutePolicy.packetSummary(buffer, count)
                        } catch (e: ErrnoException) {
                            if (e.errno != OsConstants.EAGAIN && e.errno != OsConstants.EINTR) throw e
                        }
                    }
                } catch (e: Exception) {
                    if (isCurrent(descriptor)) {
                        val message = "${e.javaClass.simpleName}: ${e.message.orEmpty().take(150)}"
                        HubRuntime.localRouteMonitorError = message
                        HubRuntime.localRouteStatus = "Local route failed: $message"
                        release()
                        stopSelf()
                    }
                }
            }
            val localServer = LocalHttpServer(
                readAsset = { path -> assets.open(path).use { it.readBytes() } },
                onListenerError = { message -> HubRuntime.localRouteStatus = message },
                allowClient = { remote, _ -> LocalRoutePolicy.permits(remote, LocalNetwork.discover()) }
            )
            synchronized(stateLock) {
                if (!active.get()) { localServer.close(); return }
                server = localServer
            }
            while (active.get()) {
                localServer.reconcile(listOf(LocalRoutePolicy.binding))
                val bound = localServer.boundAddresses().isNotEmpty()
                val result = if (bound) ServerHealthCheck.check(LocalRoutePolicy.binding) else null
                synchronized(stateLock) {
                    if (!active.get()) return
                    HubRuntime.localRouteUrl = if (bound) LocalRoutePolicy.binding.url else null
                    HubRuntime.localRouteStatus = when {
                        !bound -> localServer.lastBindError ?: "Local address listener unavailable"
                        result?.passed == true -> "Phone check passed; link-local address; PC test required; HTTPS not configured"
                        else -> "Phone check failed: ${result?.detail ?: "Unknown"}"
                    }
                }
                Thread.sleep(5000)
            }
        } catch (e: Exception) {
            synchronized(stateLock) {
                if (active.get()) HubRuntime.localRouteStatus = "Local route failed: ${e.javaClass.simpleName}: ${e.message.orEmpty().take(200)}"
            }
            release()
            stopSelf()
        }
    }

    private fun isCurrent(descriptor: ParcelFileDescriptor): Boolean = synchronized(stateLock) {
        active.get() && tunnel === descriptor
    }

    private fun release() {
        active.set(false)
        worker?.interrupt()
        synchronized(stateLock) {
            server?.close()
            server = null
            try { tunnel?.close() } catch (_: Exception) { }
            tunnel = null
            HubRuntime.localRouteUrl = null
        }
    }

    override fun onRevoke() {
        HubRuntime.localRouteStatus = "Android revoked local VPN permission"
        release()
        stopSelf()
    }

    override fun onDestroy() {
        release()
        if (!HubRuntime.localRouteStatus.startsWith("Local route failed") &&
            !HubRuntime.localRouteStatus.startsWith("Android revoked")) {
            HubRuntime.localRouteStatus = "Stopped"
        }
        super.onDestroy()
    }
}
