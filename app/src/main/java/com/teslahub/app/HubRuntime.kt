package com.teslahub.app

import android.os.Build
import org.json.JSONObject

object HubRuntime {
    @Volatile var serviceRunning = false
    @Volatile var serverAutomatic = false
    @Volatile var localRouteRunning = false
    @Volatile var localRouteAutomatic = false
    @Volatile var automationEnabled = false
    @Volatile var automationMonitorRunning = false
    @Volatile var carSelected = false
    @Volatile var bluetoothStatus = "No Tesla selected"
    @Volatile var automationStatus = "Disabled"
    @Volatile var automationLastError: String? = null
    @Volatile var shutdownRemainingSeconds: Int? = null
    @Volatile var disconnectGraceSeconds = 60
    @Volatile var stopOnDisconnect = true
    @Volatile var hotspotStrategy = "User-assisted Personal Hotspot activation"
    @Volatile var serverError = "Stopped"
    @Volatile var lastRequestError: String? = null
    @Volatile var mdnsStatus = "Stopped"
    @Volatile var addresses: List<LocalNetwork.Binding> = emptyList()
    @Volatile var serverChecks: List<ServerHealthCheck.Result> = emptyList()
    @Volatile var serverCheckedAt = 0L
    @Volatile var hotspotStatus = "Unknown"
    @Volatile var networkInfo = NetworkDiagnostics.Snapshot()
    @Volatile var localRouteStatus = "Not started (experimental)"
    @Volatile var localRouteUrl: String? = null
    @Volatile var localRoutePackets = 0L
    @Volatile var localRouteLastPacket = "None"
    @Volatile var localRouteMonitorError: String? = null
    private val browserLock = Any()
    private var browserReport = JSONObject()
    private var browserLastSeen = 0L
    private var phoneBrowserReport = JSONObject()
    private var phoneBrowserLastSeen = 0L
    private val requestLock = Any()
    private var requestCount = 0L
    private var lastClientAddress: String? = null
    private var lastRequestAt = 0L

    fun recordExternalConnection(address: String) = synchronized(requestLock) {
        requestCount++
        lastClientAddress = address
        lastRequestAt = System.currentTimeMillis()
    }

    fun resetRequests() = synchronized(requestLock) {
        requestCount = 0L
        lastClientAddress = null
        lastRequestAt = 0L
        lastRequestError = null
        synchronized(browserLock) {
            browserLastSeen = 0L
            phoneBrowserLastSeen = 0L
            browserReport = JSONObject()
            phoneBrowserReport = JSONObject()
        }
    }

    fun requestSummary(): String = synchronized(requestLock) {
        "$requestCount; last client: ${lastClientAddress ?: "None"}"
    }

    fun recordBrowser(report: JSONObject, fromPhone: Boolean) {
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
            if (fromPhone) {
                phoneBrowserReport = clean
                phoneBrowserLastSeen = System.currentTimeMillis()
            } else {
                browserReport = clean
                browserLastSeen = System.currentTimeMillis()
            }
        }
    }

    fun pingBrowser(fromPhone: Boolean) {
        synchronized(browserLock) {
            if (fromPhone) phoneBrowserLastSeen = System.currentTimeMillis()
            else browserLastSeen = System.currentTimeMillis()
        }
    }

    fun browserConnected(): Boolean = synchronized(browserLock) {
        browserLastSeen > 0 && System.currentTimeMillis() - browserLastSeen < 15_000
    }

    fun stateJson(fromPhone: Boolean = false): JSONObject {
        val currentAddresses = addresses
        val browser = synchronized(browserLock) {
            JSONObject((if (fromPhone) phoneBrowserReport else browserReport).toString())
        }
        return JSONObject().apply {
            put("serviceRunning", serviceRunning)
            put("serverListening", currentAddresses.isNotEmpty())
            put("serverRunning", currentAddresses.isNotEmpty() && serverChecks.any { it.passed })
            put("serverError", serverError)
            put("localRouteStatus", localRouteStatus)
            put("localRouteUrl", localRouteUrl ?: JSONObject.NULL)
            put("localRoutePackets", localRoutePackets)
            put("localRouteLastPacket", localRouteLastPacket)
            put("localRouteMonitorError", localRouteMonitorError ?: JSONObject.NULL)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("serverCheckedAt", serverCheckedAt)
            put("serverChecks", org.json.JSONArray().apply {
                serverChecks.forEach { result ->
                    put(JSONObject().put("url", result.binding.url).put("passed", result.passed).put("detail", result.detail))
                }
            })
            synchronized(requestLock) {
                put("externalConnectionCount", requestCount)
                put("lastExternalClientAddress", lastClientAddress ?: JSONObject.NULL)
                put("lastExternalConnectionAt", lastRequestAt)
            }
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
            put("vpnActive", if (networkInfo.checked) networkInfo.vpnActive else JSONObject.NULL)
            put("connectedWifi", org.json.JSONArray().apply {
                networkInfo.wifiLinks.forEach { link ->
                    put(JSONObject().put("interface", link.interfaceName)
                        .put("addresses", org.json.JSONArray(link.addresses))
                        .put("gateways", org.json.JSONArray(link.gateways)))
                }
            })
            put("bluetooth", bluetoothStatus)
            put("automation", JSONObject().put("enabled", automationEnabled)
                .put("monitorRunning", automationMonitorRunning).put("carSelected", carSelected)
                .put("state", automationStatus).put("graceSeconds", disconnectGraceSeconds)
                .put("stopOnDisconnect", stopOnDisconnect)
                .put("shutdownRemainingSeconds", shutdownRemainingSeconds ?: JSONObject.NULL)
                .put("serverStartedAutomatically", serverAutomatic)
                .put("localRouteStartedAutomatically", localRouteAutomatic)
                .put("hotspotStrategy", hotspotStrategy)
                .put("lastError", automationLastError ?: JSONObject.NULL))
            put("requestFromPhone", fromPhone)
            put("externalBrowserConnected", browserConnected())
            put("browserConnected", if (fromPhone) synchronized(browserLock) {
                phoneBrowserLastSeen > 0 && System.currentTimeMillis() - phoneBrowserLastSeen < 15_000
            } else browserConnected())
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
