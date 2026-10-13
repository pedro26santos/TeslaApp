package com.teslahub.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Only system Bluetooth/boot/package events can enter the persisted opt-in path. */
class ConnectionReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = HubPreferences(context).apply { publish() }
        if (!prefs.automationEnabled || prefs.carAddress == null || !TeslaBluetoothMonitor.hasPermission(context)) return
        if (intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) {
            val device = TeslaBluetoothMonitor.device(intent) ?: return
            try { if (device.address != prefs.carAddress) return } catch (_: SecurityException) { return }
            if (intent.getIntExtra(BluetoothDevice.EXTRA_TRANSPORT, BluetoothDevice.TRANSPORT_AUTO) == BluetoothDevice.TRANSPORT_LE) return
        } else if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (NetworkControl.resumeMonitoring(context, intent.action == BluetoothDevice.ACTION_ACL_CONNECTED) != null) {
            // Background starts remain subject to Android/ColorOS policy. A user-opened app
            // is a supported recovery path; no background activity or hidden permission API.
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("teslahub_attention", "Tesla Hub attention", NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(context, 33, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(4, Notification.Builder(context, "teslahub_attention")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Resume Tesla Hub")
                .setContentText("Open Tesla Hub to resume Bluetooth connection monitoring")
                .setContentIntent(open).setAutoCancel(true).build())
        }
    }
}
