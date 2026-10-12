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
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

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
        addButton(column, "Stop local server") { stopService(Intent(this, HubService::class.java)) }
        addButton(column, "Copy fallback URL") {
            val url = HubRuntime.addresses.firstOrNull()?.url ?: return@addButton
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Tesla Hub URL", url))
        }
        addButton(column, "Open hotspot settings") {
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        }
        val note = TextView(this).apply {
            text = "Phase 1: start the server manually. Android Auto and Bluetooth automation are planned for later phases. Keep the server notification active during the Tesla test."
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

    override fun onStart() {
        super.onStart()
        handler.post(refresh)
    }

    override fun onStop() {
        handler.removeCallbacks(refresh)
        super.onStop()
    }

    private fun updateStatus() {
        val urls = HubRuntime.addresses.joinToString("\n") { "  ${it.interfaceName}: ${it.url}" }
        status.text = """
            Tesla Bluetooth: Not configured (Phase 2)
            Hotspot: ${HubRuntime.hotspotStatus}
            Local Server: ${if (HubRuntime.addresses.isNotEmpty()) "Running" else "Stopped"}
            Server detail: ${HubRuntime.serverError}
            Last request error: ${HubRuntime.lastRequestError ?: "None"}
            Tesla Browser: ${if (HubRuntime.browserConnected()) "Connected" else "Disconnected"}
            Android Auto: Not installed (Phase 3)
            mDNS: ${HubRuntime.mdnsStatus}

            Preferred address (unverified):
              http://teslahub.local:8765/
            Fallback address:
              ${HubRuntime.addresses.firstOrNull()?.url ?: "Unavailable — enable hotspot and start server"}
            All local addresses:
            ${if (urls.isBlank()) "  No Wi-Fi/hotspot interface detected" else urls}
        """.trimIndent()
    }
}
