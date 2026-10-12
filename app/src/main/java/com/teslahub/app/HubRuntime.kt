package com.teslahub.app

import android.os.Build
import org.json.JSONObject

object HubRuntime {
    @Volatile var serviceRunning = false
    @Volatile var serverError = "Stopped"
    @Volatile var lastRequestError: String? = null
    @Volatile var mdnsStatus = "Stopped"
    @Volatile var addresses: List<LocalNetwork.Binding> = emptyList()
    @Volatile var hotspotStatus = "Unknown"
    private val browserLock = Any()
    private var browserReport = JSONObject()
    private var browserLastSeen = 0L

    fun recordBrowser(report: JSONObject) {
        val clean = JSONObject()
        for (key in listOf("innerWidth", "innerHeight", "devicePixelRatio", "screenWidth",
            "screenHeight", "userAgent", "platform", "orientation", "language", "secureContext",
            "webSocket", "webRTC", "webCodecs", "h264WebCodecs", "mediaSource", "eme",
            "widevine", "clearKey", "h264", "h265", "aac", "cookiesEnabled", "localStorage",
            "serviceWorker", "error")) {
            if (report.has(key) && !report.isNull(key)) {
                val value = report.opt(key)
                if (value is Boolean || value is Number || value is String) {
                    clean.put(key, if (value is String) value.take(400) else value)
                }
            }
        }
        synchronized(browserLock) {
            browserReport = clean
            browserLastSeen = System.currentTimeMillis()
        }
    }

    fun pingBrowser() {
        synchronized(browserLock) { browserLastSeen = System.currentTimeMillis() }
    }

    fun browserConnected(): Boolean = synchronized(browserLock) {
        browserLastSeen > 0 && System.currentTimeMillis() - browserLastSeen < 15_000
    }

    fun stateJson(): JSONObject {
        val currentAddresses = addresses
        val browser = synchronized(browserLock) { JSONObject(browserReport.toString()) }
        return JSONObject().apply {
            put("serviceRunning", serviceRunning)
            put("serverRunning", currentAddresses.isNotEmpty())
            put("serverError", serverError)
            put("lastRequestError", lastRequestError ?: JSONObject.NULL)
            put("mdnsStatus", mdnsStatus)
            put("preferredAddress", "http://teslahub.local:8765/")
            put("fallbackAddress", currentAddresses.firstOrNull()?.url ?: JSONObject.NULL)
            put("addresses", org.json.JSONArray().apply {
                currentAddresses.forEach { binding ->
                    put(JSONObject().put("interface", binding.interfaceName).put("url", binding.url)
                        .put("prefixLength", binding.prefixLength))
                }
            })
            put("hotspot", hotspotStatus)
            put("bluetooth", "Not configured (Phase 2)")
            put("browserConnected", browserConnected())
            put("browser", browser)
            put("phone", JSONObject().put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL).put("android", Build.VERSION.RELEASE)
                .put("sdk", Build.VERSION.SDK_INT)
                .put("colorOs", "Unavailable through public Android API"))
            put("androidAuto", "Not installed (Phase 3)")
            put("stream", "Not installed (Phase 4)")
        }
    }
}
