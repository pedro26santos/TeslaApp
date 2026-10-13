package com.teslahub.app

import android.content.Context

class HubPreferences(context: Context) {
    private val store = context.getSharedPreferences("teslahub_settings", Context.MODE_PRIVATE)
    val carAddress: String? get() = store.getString("car_address", null)
    val carName: String get() = store.getString("car_name", "Selected Tesla") ?: "Selected Tesla"
    var automationEnabled: Boolean
        get() = store.getBoolean("automation_enabled", false)
        set(value) { store.edit().putBoolean("automation_enabled", value).apply(); publish() }
    var graceSeconds: Int
        get() = store.getInt("grace_seconds", 60).coerceIn(10, 600)
        set(value) { require(value in 10..600); store.edit().putInt("grace_seconds", value).apply(); publish() }
    var stopOnDisconnect: Boolean
        get() = store.getBoolean("stop_on_disconnect", true)
        set(value) { store.edit().putBoolean("stop_on_disconnect", value).apply(); publish() }

    fun selectCar(address: String, name: String) {
        // Changing the target requires enabling automation again for that new device.
        store.edit().putString("car_address", address).putString("car_name", name.take(80))
            .putBoolean("automation_enabled", false).apply()
        publish()
    }

    fun publish() {
        HubRuntime.automationEnabled = automationEnabled
        HubRuntime.carSelected = carAddress != null
        HubRuntime.disconnectGraceSeconds = graceSeconds
        HubRuntime.stopOnDisconnect = stopOnDisconnect
    }
}
