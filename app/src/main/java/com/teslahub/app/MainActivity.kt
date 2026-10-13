package com.teslahub.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(13, 17, 22)
        window.navigationBarColor = Color.rgb(13, 17, 22)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(13, 17, 22))
        }
        val title = TextView(this).apply {
            text = "TESLA HUB"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        column.addView(title)
        addButton(column, "Connection settings") { startActivity(Intent(this, ConnectionSettingsActivity::class.java)) }
        addButton(column, "Start Tesla Hub") { startManualServer(); requestLocalRoute() }
        addButton(column, "Stop Tesla Hub") {
            NetworkControl.disableAutomation(this)
            NetworkControl.stopLocalRoute(this)
            stopService(Intent(this, HubService::class.java))
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(222, 228, 235))
            setLineSpacing(5f, 1f)
        }
        column.addView(status)
        addButton(column, "Start local server") {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
            try {
                startForegroundService(Intent(this, HubService::class.java))
            } catch (e: Exception) {
                HubRuntime.serverError = "Server failed to start: ${e.message ?: e.javaClass.simpleName}"
                updateStatus()
            }
        }
        addButton(column, "Stop local server") {
            NetworkControl.disableAutomation(this)
            stopService(Intent(this, HubService::class.java))
        }
        addButton(column, "Copy fallback URL") {
            val url = HubRuntime.addresses.firstOrNull()?.url ?: return@addButton
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Tesla Hub URL", url))
        }
        addButton(column, "Open launcher on phone") {
            val url = HubRuntime.addresses.firstOrNull()?.url
            if (url == null) {
                Toast.makeText(this, "Enable hotspot and start the server first", Toast.LENGTH_SHORT).show()
            } else {
                val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try { startActivity(browser) } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(this, "No browser installed; use Copy fallback URL", Toast.LENGTH_LONG).show()
                }
            }
        }
        addButton(column, "Copy connection diagnostics") {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Tesla Hub connection diagnostics", HubRuntime.stateJson().toString(2)))
            Toast.makeText(this, "Connection diagnostics copied", Toast.LENGTH_SHORT).show()
        }
        addButton(column, "Open hotspot settings") {
            startActivity(HotspotAssistance.settingsIntent(this))
        }
        addButton(column, "Start local route test (experimental)") {
            android.app.AlertDialog.Builder(this)
                .setTitle("Local route test")
                .setMessage("This creates a local address on your phone using Android's VPN API. It uses no remote VPN server. Android permits one active VPN, so starting it can replace another VPN. Test it on your PC first; trusted HTTPS for Tesla is not configured yet.")
                .setPositiveButton("Continue") { _, _ ->
                    val consent = VpnService.prepare(this)
                    if (consent == null) startLocalRoute() else startActivityForResult(consent, 42)
                }
                .setNegativeButton("Cancel", null).show()
        }
        addButton(column, "Stop local route test") {
            NetworkControl.disableAutomation(this)
            NetworkControl.stopLocalRoute(this)
        }
        addButton(column, "Copy local route test URL") {
            val url = HubRuntime.localRouteUrl
            if (url == null) Toast.makeText(this, "Start the local route test first", Toast.LENGTH_SHORT).show()
            else getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Local route test URL", url))
        }
        val note = TextView(this).apply {
            text = "Phase 2: select your paired Tesla in Connection settings to enable automatic startup. Personal Hotspot is enabled in phone settings. Android Auto is planned for Phase 3. Keep the service notifications active."
            textSize = 14f
            setTextColor(Color.rgb(148, 160, 174))
            setPadding(0, 18, 0, 0)
        }
        column.addView(note)
        setContentView(ScrollView(this).apply {
            addView(column)
            if (Build.VERSION.SDK_INT >= 35) {
                setOnApplyWindowInsetsListener { view: View, insets: WindowInsets ->
                    val bars = insets.getInsets(WindowInsets.Type.systemBars())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                    insets
                }
            }
        })
    }

    private fun addButton(parent: LinearLayout, label: String, onClick: () -> Unit) {
        parent.addView(Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun startLocalRoute() {
        try {
            startForegroundService(Intent(this, LocalRouteService::class.java))
        } catch (e: Exception) {
            HubRuntime.localRouteStatus = "Local route failed to start: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun startManualServer() {
        try { startForegroundService(Intent(this, HubService::class.java)) }
        catch (e: Exception) { HubRuntime.serverError = "Server failed to start: ${e.javaClass.simpleName}" }
    }

    private fun requestLocalRoute() {
        val consent = VpnService.prepare(this)
        if (consent == null) startLocalRoute()
        else android.app.AlertDialog.Builder(this).setTitle("Tesla local connection")
            .setMessage("This starts the phone's local VPN address. It uses no remote VPN server. Android supports one active VPN; approving this can replace another VPN.")
            .setPositiveButton("Continue") { _, _ -> startActivityForResult(consent, 42) }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42) {
            if (resultCode == RESULT_OK) startLocalRoute()
            else HubRuntime.localRouteStatus = "Android VPN permission was not approved"
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = HubPreferences(this).apply { publish() }
        if (prefs.automationEnabled && !HubRuntime.automationMonitorRunning && TeslaBluetoothMonitor.hasPermission(this))
            NetworkControl.resumeMonitoring(this)
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }

    private fun updateStatus() {
        val urls = HubRuntime.addresses.joinToString("\n            ") {
            "  ${it.interfaceName}${if (it.connectedWifi) " (phone's connected Wi-Fi)" else ""}: ${it.url}"
        }
        val checks = HubRuntime.serverChecks.joinToString("\n            ") { "  ${it.binding.key}: ${it.detail}" }
        val running = when {
            HubRuntime.addresses.isEmpty() -> "Stopped"
            HubRuntime.serverChecks.any { it.passed } -> "Running (phone check passed)"
            HubRuntime.serverChecks.isEmpty() -> "Listening — checking response"
            else -> "Listening — phone check failed"
        }
        val statusText = """
            App version: ${BuildConfig.VERSION_NAME}
            Tesla address: ${HubRuntime.localRouteUrl ?: "http://${LocalRoutePolicy.ADDRESS}:8765/ (start Tesla Hub first)"}
            Tesla Bluetooth: ${HubRuntime.bluetoothStatus}
            Automatic connection: ${HubRuntime.automationStatus}
            Disconnect delay: ${HubRuntime.disconnectGraceSeconds}s; remaining: ${HubRuntime.shutdownRemainingSeconds?.let { "${it}s" } ?: "None"}
            Automation error: ${HubRuntime.automationLastError ?: "None"}
            Hotspot control: ${HubRuntime.hotspotStrategy}
            Hotspot: ${HubRuntime.hotspotStatus}
            Local Server: $running
            Server detail: ${HubRuntime.serverError}
            Server control: ${if (!HubRuntime.serviceRunning) "Stopped" else if (HubRuntime.serverAutomatic) "Automatic" else "Manual"}
            Phone server check:
            ${checks.ifBlank { "  Not checked" }}
            Connections from other devices: ${HubRuntime.requestSummary()}
            VPN active: ${if (!HubRuntime.networkInfo.checked) "Unknown" else if (HubRuntime.networkInfo.vpnActive) "Yes" else "No"}
            Last request error: ${HubRuntime.lastRequestError ?: "None"}
            Tesla / external browser: ${if (HubRuntime.browserConnected()) "Connected" else "Disconnected"}
            Android Auto: Not installed (Phase 3)
            mDNS: ${HubRuntime.mdnsStatus}

            Preferred address (unverified):
              http://teslahub.local:8765/
            Fallback address:
              ${HubRuntime.addresses.firstOrNull()?.url ?: "Unavailable — enable hotspot and start server"}
            All local addresses:
            ${if (urls.isBlank()) "  No Wi-Fi/hotspot interface detected" else urls}

            Experimental local route (PC test first):
              ${HubRuntime.localRouteStatus}
              ${HubRuntime.localRouteUrl ?: "No test URL"}
            Packets arriving in TUN: ${HubRuntime.localRoutePackets}
            Last route packet: ${HubRuntime.localRouteLastPacket}
            Route monitor error: ${HubRuntime.localRouteMonitorError ?: "None"}
        """.trimIndent()
        // Replacing identical text every second relayouts ScrollView and can move the buttons
        // while the user is scrolling. Only update when a displayed value actually changes.
        if (status.text.toString() != statusText) status.text = statusText
    }
}
