package com.teslahub.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ReceiverActivity : Activity() {
    private lateinit var details: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            AndroidAutoController.refresh(this@ReceiverActivity)
            val text = AndroidAutoController.stateJson().toString(2)
            if (details.text.toString() != text) details.text = text
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 28)
            setBackgroundColor(Color.rgb(13, 17, 22))
        }
        fun text(value: String, size: Float = 16f) = TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.WHITE); setPadding(0, 12, 0, 12)
        }
        fun button(label: String, action: () -> Unit) {
            column.addView(Button(this).apply { text = label; setOnClickListener { action() } })
        }
        column.addView(text("ANDROID AUTO — PHONE RECEIVER TEST", 24f))
        column.addView(text("Phase 3 tests real Android Auto on this OPPO using a separate Open Headunit receiver. Tesla browser projection follows in Phase 4.\n\nIn Android Auto settings, tap Version 10 times and use the three-dot menu → Start headunit server. Complete receiver setup, then start the phone receiver below. The 17.4+ path uses localhost and does not replace Tesla Hub's VPN.\n\nThe initial receiver profile is H.264, 720p, 30 FPS. Reported announcement settings and actual decoded video are shown separately. Audio routing still needs a listening test."))
        button("Open Android Auto settings") { AndroidAutoController.openAndroidAutoSettings(this) }
        button("Open receiver setup") { AndroidAutoController.openReceiverSetup(this) }
        button("Start phone receiver") { AndroidAutoController.startPhoneReceiver(this) }
        button("Stop phone receiver") { AndroidAutoController.stopPhoneReceiver(this) }
        button("Refresh receiver diagnostics") { AndroidAutoController.refresh(this, true) }
        button("Copy receiver diagnostics") {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                ClipData.newPlainText("Tesla Hub receiver diagnostics", AndroidAutoController.stateJson().toString(2)))
        }
        button("Back to Tesla Hub") { finish() }
        details = text("")
        column.addView(details)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onStart() { super.onStart(); handler.post(refresh) }
    override fun onStop() { handler.removeCallbacks(refresh); super.onStop() }
}
