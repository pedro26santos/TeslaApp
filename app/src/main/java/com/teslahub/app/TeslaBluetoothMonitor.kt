package com.teslahub.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper

/** Observes existing classic Bluetooth connections. Does not connect, scan, or change audio. */
@SuppressLint("MissingPermission")
class TeslaBluetoothMonitor(private val context: Context, private val address: String,
    private val changed: (ConnectionSession.Observation, String) -> Unit) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val handler = Handler(Looper.getMainLooper())
    private val profiles = mutableMapOf<Int, BluetoothProfile>()
    private val requested = mutableSetOf<Int>()
    private var aclConnected = false
    private var closed = false
    private var registered = false
    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            handler.post {
                if (closed) { try { adapter?.closeProfileProxy(profile, proxy) } catch (_: Exception) { } }
                else { profiles[profile] = proxy; refresh() }
            }
        }
        override fun onServiceDisconnected(profile: Int) { handler.post { profiles.remove(profile); requested.remove(profile); refresh() } }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (closed) return
            if (!hasPermission(context)) { refresh(); return }
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) != BluetoothAdapter.STATE_ON)
                    aclConnected = false
            } else {
                val device = device(intent) ?: return
                try { if (device.address != address) return } catch (_: SecurityException) { refresh(); return }
                if (intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_AUTO) == BluetoothDevice.TRANSPORT_LE) return
                when (intent.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> aclConnected = true
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> aclConnected = false
                }
            }
            refresh()
        }
    }

    fun start() {
        check(hasPermission(context)) { "Nearby Devices permission is required" }
        if (adapter == null) { changed(ConnectionSession.Observation.UNKNOWN, "Bluetooth unavailable"); return }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else context.registerReceiver(receiver, filter)
        registered = true
        refresh()
    }

    fun refresh() {
        if (closed) return
        try {
            if (!hasPermission(context)) { changed(ConnectionSession.Observation.UNKNOWN, "Nearby Devices permission missing"); return }
            if (adapter == null) { changed(ConnectionSession.Observation.UNKNOWN, "Bluetooth unavailable"); return }
            if (!adapter.isEnabled) { changed(ConnectionSession.Observation.DISCONNECTED, "Bluetooth off"); return }
            for (profile in listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
                if (profile !in profiles && profile !in requested) {
                    if (adapter.getProfileProxy(context, profileListener, profile)) requested.add(profile)
                    else HubRuntime.automationLastError = "Bluetooth profile query unavailable; waiting for connection events"
                }
            }
            val profileConnected = profiles.values.any { proxy -> proxy.connectedDevices.any { it.address == address } }
            when {
                aclConnected || profileConnected -> changed(ConnectionSession.Observation.CONNECTED, "Selected Tesla connected")
                profiles.size == 2 -> changed(ConnectionSession.Observation.DISCONNECTED, "Selected Tesla disconnected")
                else -> changed(ConnectionSession.Observation.UNKNOWN, "Checking selected Tesla connection")
            }
        } catch (e: Exception) {
            HubRuntime.automationLastError = "Bluetooth query failed: ${e.javaClass.simpleName}"
            changed(ConnectionSession.Observation.UNKNOWN, "Bluetooth state unavailable")
        }
    }

    // This hint comes only from the manifest receiver after it matched the selected device
    // in a protected system ACL broadcast. It bridges the initial profile-query delay.
    fun connectedEvent() { aclConnected = true; refresh() }

    fun close() {
        closed = true
        if (registered) { context.unregisterReceiver(receiver); registered = false }
        for ((profile, proxy) in profiles) {
            try { adapter?.closeProfileProxy(profile, proxy) } catch (_: Exception) { }
        }
        profiles.clear()
        requested.clear()
    }

    companion object {
        fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 31 ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

        fun device(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else @Suppress("DEPRECATION") (intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice)
    }
}
