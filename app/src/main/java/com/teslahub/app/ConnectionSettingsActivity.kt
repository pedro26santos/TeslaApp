package com.teslahub.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class ConnectionSettingsActivity : Activity() {
    private lateinit var prefs: HubPreferences
    private lateinit var details: TextView
    private lateinit var automatic: Switch
    private lateinit var stopAfterDisconnect: Switch
    private var updating = false
    private var pendingEnable = false
    private val handler = Handler(Looper.getMainLooper())
    private val refreshUi = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = HubPreferences(this).apply { publish() }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(13, 17, 22))
        }
        column.addView(TextView(this).apply { text = "CONNECTION SETTINGS"; textSize = 24f; setTextColor(Color.WHITE) })
        details = TextView(this).apply { textSize = 16f; setTextColor(Color.rgb(222, 228, 235)); setPadding(0, 24, 0, 24) }
        column.addView(details)
        button(column, "Select paired Tesla") { withBluetoothPermission(enable = false) }
        automatic = Switch(this).apply {
            text = "Automatic Tesla connection"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, checked ->
                if (!updating) {
                    if (checked) withBluetoothPermission(enable = true)
                    else { NetworkControl.disableAutomation(this@ConnectionSettingsActivity); refresh() }
                }
            }
        }
        column.addView(automatic)
        stopAfterDisconnect = Switch(this).apply {
            text = "Stop automatic services after disconnect"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 24)
            setOnCheckedChangeListener { _, checked -> if (!updating) { prefs.stopOnDisconnect = checked; refresh() } }
        }
        column.addView(stopAfterDisconnect)
        button(column, "Change disconnect delay") { changeDelay() }
        button(column, "Resume automatic monitoring") { withBluetoothPermission(enable = true) }
        button(column, "Open Personal Hotspot settings") { startActivity(HotspotAssistance.settingsIntent(this)) }
        button(column, "Back to Tesla Hub") { finish() }
        column.addView(TextView(this).apply {
            text = "Automatic mode starts the local server and Tesla connection when the selected paired car connects. Only services started automatically stop after the delay; manually started services stay running.\n\nOn this OPPO, enable the internet-sharing Personal Hotspot in phone settings. Tesla Hub does not switch it off. Android Auto receiver testing is available on the phone; Tesla browser projection follows in Phase 4."
            textSize = 15f
            setTextColor(Color.rgb(148, 160, 174))
            setPadding(0, 24, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(column) })
        refresh()
        if (intent.getBooleanExtra(OPEN_HOTSPOT, false)) startActivity(HotspotAssistance.settingsIntent(this))
    }

    override fun onResume() { super.onResume(); if (::prefs.isInitialized) refresh() }
    override fun onStart() { super.onStart(); handler.post(refreshUi) }
    override fun onStop() { handler.removeCallbacks(refreshUi); super.onStop() }

    private fun button(parent: LinearLayout, label: String, action: () -> Unit) {
        parent.addView(Button(this).apply { text = label; setOnClickListener { action() } })
    }

    private fun refresh() {
        updating = true
        automatic.isChecked = prefs.automationEnabled
        stopAfterDisconnect.isChecked = prefs.stopOnDisconnect
        val value = "Tesla: ${if (prefs.carAddress == null) "Not selected" else prefs.carName}\n" +
            "Disconnect delay: ${prefs.graceSeconds} seconds\n" +
            "Nearby Devices: ${if (TeslaBluetoothMonitor.hasPermission(this)) "Allowed" else "Permission needed"}\n" +
            "Monitor: ${if (HubRuntime.automationMonitorRunning) "Running" else "Stopped"}\n" +
            "${HubRuntime.automationLastError ?: "Hotspot control: user assisted"}"
        if (details.text.toString() != value) details.text = value
        updating = false
    }

    private fun withBluetoothPermission(enable: Boolean) {
        pendingEnable = enable
        if (!TeslaBluetoothMonitor.hasPermission(this)) {
            if (Build.VERSION.SDK_INT >= 31) requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 71)
            reflectSavedMode()
        } else if (enable) prepareAutomation() else chooseDevice()
    }

    private fun reflectSavedMode() { updating = true; automatic.isChecked = prefs.automationEnabled; updating = false }

    @SuppressLint("MissingPermission")
    private fun chooseDevice() {
        try {
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
            if (adapter == null) { message("Bluetooth unavailable on this device"); return }
            val devices = adapter.bondedDevices.filter { it.type != BluetoothDevice.DEVICE_TYPE_LE }
                .sortedBy { it.name.orEmpty().lowercase() }
            if (devices.isEmpty()) { message("Pair the phone with the Tesla's phone/audio Bluetooth in Android settings first"); return }
            val labels = devices.map { "${it.name ?: "Unnamed device"} (${it.address.takeLast(5)})" }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Choose Tesla phone/audio pairing").setItems(labels) { _, index ->
                NetworkControl.disableAutomation(this)
                prefs.selectCar(devices[index].address, devices[index].name ?: "Selected Tesla")
                HubRuntime.bluetoothStatus = "Tesla selected; enable automatic connection to monitor"
                refresh()
            }.setNegativeButton("Cancel", null).show()
        } catch (e: Exception) { message("Paired device list unavailable: ${e.javaClass.simpleName}") }
    }

    private fun prepareAutomation() {
        if (prefs.carAddress == null) { message("Select your paired Tesla first"); reflectSavedMode(); return }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 72)
            reflectSavedMode()
            return
        }
        if (!getSystemService(NotificationManager::class.java).areNotificationsEnabled()) {
            message("Enable Tesla Hub notifications for the connection monitor and hotspot prompts")
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            reflectSavedMode()
            return
        }
        val consent = VpnService.prepare(this)
        if (consent != null) {
            AlertDialog.Builder(this).setTitle("Tesla local connection")
                .setMessage("Automatic mode uses the same local VPN address already tested in your Tesla. It has no remote VPN server. Android supports one active VPN, so approving this connection can replace another VPN.")
                .setPositiveButton("Continue") { _, _ -> startActivityForResult(consent, 73) }
                .setNegativeButton("Cancel") { _, _ -> reflectSavedMode() }.setOnCancelListener { reflectSavedMode() }.show()
        } else enableAutomation()
    }

    private fun enableAutomation() {
        prefs.automationEnabled = true
        HubRuntime.automationLastError = null
        val error = NetworkControl.resumeMonitoring(this)
        if (error != null) { prefs.automationEnabled = false; message(error) }
        refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 71) {
            if (TeslaBluetoothMonitor.hasPermission(this)) {
                if (pendingEnable) prepareAutomation() else chooseDevice()
            } else message("Nearby Devices permission was not granted; manual launcher still works")
        } else if (requestCode == 72) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) prepareAutomation()
            else message("Notifications are needed for automatic mode; manual launcher still works")
        }
        refresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 73) {
            if (resultCode == RESULT_OK) enableAutomation()
            else message("Local VPN permission was not approved; automatic mode remains disabled")
            refresh()
        }
    }

    private fun changeDelay() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; setText(prefs.graceSeconds.toString()) }
        val dialog = AlertDialog.Builder(this).setTitle("Disconnect delay in seconds (10–600)")
            .setView(input).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                if (value == null || value !in 10..600) input.error = "Enter 10 to 600 seconds"
                else { prefs.graceSeconds = value; refresh(); dialog.dismiss() }
            }
        }
        dialog.show()
    }

    private fun message(value: String) { Toast.makeText(this, value, Toast.LENGTH_LONG).show() }

    companion object { const val OPEN_HOTSPOT = "open_hotspot" }
}
