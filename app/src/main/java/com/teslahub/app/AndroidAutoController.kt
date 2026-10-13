package com.teslahub.app

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject

/** Original client for the separate, source-built receiver's documented intent protocol. */
object AndroidAutoController {
    const val RECEIVER_PACKAGE = "com.teslahub.receiver"
    private const val ANDROID_AUTO_PACKAGE = "com.google.android.projection.gearhead"
    private const val PREFIX = "com.andrerinas.openheadunit."
    private val component = ComponentName(RECEIVER_PACKAGE, "com.andrerinas.openheadunit.automation.AutomationReceiver")
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var queryId = 0L
    private var inFlight = false
    private var attemptedAt = -8_000L
    private var observedAt: Long? = null
    private var session = JSONObject()
    private var receiverVersion: String? = null
    private var androidAutoVersion: String? = null
    private var queryError: String? = null
    private var commandStatus = "Not started"

    private fun version(context: Context, packageName: String): String? = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0).versionName
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) { null }

    fun refresh(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        handler.post {
            val now = SystemClock.elapsedRealtime()
            val id: Long
            synchronized(lock) {
                receiverVersion = version(app, RECEIVER_PACKAGE)
                androidAutoVersion = version(app, ANDROID_AUTO_PACKAGE)
                if (receiverVersion == null) {
                    queryId++; inFlight = false
                    session = JSONObject(); observedAt = null; queryError = null
                    return@post
                }
                if (inFlight || !force && now - attemptedAt < 8_000) return@post
                inFlight = true; attemptedAt = now; id = ++queryId
            }
            val timeout = Runnable { synchronized(lock) {
                if (id == queryId && inFlight) {
                    inFlight = false
                    queryError = "Receiver query timed out; open receiver setup"
                }
            } }
            handler.postDelayed(timeout, 4000)
            val response = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    handler.removeCallbacks(timeout)
                    synchronized(lock) {
                        if (id != queryId || !inFlight) return
                        inFlight = false
                        try {
                            val raw = resultData ?: error("No receiver reply")
                            check(raw.length <= 8192) { "Receiver reply exceeded limit" }
                            val data = JSONObject(raw)
                            check(data.has("connected") && data.has("state")) { "Unsupported receiver reply" }
                            session = cleanSession(data)
                            observedAt = SystemClock.elapsedRealtime()
                            queryError = null
                        } catch (e: Exception) {
                            queryError = "Receiver reply unavailable: ${e.javaClass.simpleName}"
                            observedAt = null; session = JSONObject()
                        }
                    }
                }
            }
            try {
                app.sendOrderedBroadcast(Intent(PREFIX + "ACTION_QUERY_STATE").setComponent(component), null,
                    response, handler, Activity.RESULT_CANCELED, null, null)
            } catch (e: Exception) {
                handler.removeCallbacks(timeout)
                synchronized(lock) { inFlight = false; queryError = "Receiver query blocked: ${e.javaClass.simpleName}" }
            }
        }
    }

    private fun cleanSession(data: JSONObject): JSONObject = JSONObject().apply {
        put("connected", data.optBoolean("connected", false))
        put("state", data.optString("state", "Unknown").take(60))
        put("build", data.optString("commit", "Unknown").take(80))
        val probe = data.optJSONObject("teslaHubProbe")
        put("probeAvailable", probe?.optInt("probeVersion") == 1)
        if (probe?.optInt("probeVersion") == 1) {
            put("transportStarted", probe.optBoolean("transportStarted", false))
            put("framesRendered", probe.optLong("framesRendered", 0).coerceIn(0, 1_000_000_000))
            for (key in listOf("decodedWidth", "decodedHeight", "announcedWidth", "announcedHeight", "announcedDpi", "announcedFps")) {
                val number = probe.optInt(key, -1)
                put(key, if (number in 1..16_384) number else JSONObject.NULL)
            }
            val age = probe.optLong("lastFrameAgeMs", -1)
            put("lastFrameAgeMs", if (age >= 0) age else JSONObject.NULL)
            put("codec", if (probe.isNull("codec")) JSONObject.NULL else probe.optString("codec").take(60))
            val sinks = JSONObject()
            val reported = probe.optJSONObject("audioSinks") ?: JSONObject()
            reported.keys().asSequence().take(8).forEach { key ->
                if (key.toIntOrNull() != null) sinks.put(key.take(8), reported.optString(key).take(40))
            }
            put("audioSinks", sinks)
        }
    }

    fun stateJson(): JSONObject = synchronized(lock) {
        val fresh = ReceiverSessionEvidence.fresh(observedAt, SystemClock.elapsedRealtime())
        val connected = receiverVersion != null && fresh && session.optBoolean("connected", false)
        val video = fresh && ReceiverSessionEvidence.videoDecoded(connected,
            session.optBoolean("transportStarted", false), session.optLong("framesRendered", 0))
        val label = when {
            receiverVersion == null -> "Receiver app not installed (Phase 3)"
            androidAutoVersion == null -> "Android Auto not installed on phone"
            queryError != null -> queryError!!
            !fresh -> "Receiver installed; session state not yet available"
            video -> "Android Auto video decoded on phone; browser projection pending"
            connected -> "Receiver ${session.optString("state")}; video not confirmed"
            else -> "Receiver ${session.optString("state", "Disconnected")}; not projecting"
        }
        JSONObject().put("label", label).put("receiverInstalled", receiverVersion != null)
            .put("receiverVersion", receiverVersion ?: JSONObject.NULL)
            .put("androidAutoVersion", androidAutoVersion ?: JSONObject.NULL)
            .put("fresh", fresh).put("connected", connected).put("videoDecodedOnPhone", video)
            .put("command", commandStatus).put("error", queryError ?: JSONObject.NULL)
            .put("browserProjection", false)
            .put("session", if (fresh) JSONObject(session.toString()) else JSONObject())
    }

    fun label(): String = stateJson().getString("label")

    fun openAndroidAutoSettings(activity: Activity) = launch(activity,
        Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(ANDROID_AUTO_PACKAGE))

    fun openReceiverSetup(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(RECEIVER_PACKAGE)
        if (intent == null) { recordCommand("Install the source-built receiver app first"); return }
        launch(activity, intent)
    }

    fun startPhoneReceiver(activity: Activity) {
        val aa = version(activity, ANDROID_AUTO_PACKAGE)
        if (!ReceiverSessionEvidence.supportsServerSelfMode(aa)) {
            recordCommand("This test requires Android Auto 17.4+ headunit-server mode; installed: ${aa ?: "None"}")
            return
        }
        if (version(activity, RECEIVER_PACKAGE) == null) { recordCommand("Install the receiver app first"); return }
        synchronized(lock) { queryId++; inFlight = false; observedAt = null; session = JSONObject() }
        // The inspected 17.4+ upstream path dials localhost and creates no dummy VPN.
        if (launch(activity, Intent(PREFIX + "ACTION_START_SELF_MODE")
            .setClassName(RECEIVER_PACKAGE, "com.andrerinas.openheadunit.main.AutomationActivity"))) {
            recordCommand("Receiver start requested; real video requires a successful session and decoded frames")
        }
    }

    fun stopPhoneReceiver(context: Context) {
        if (version(context, RECEIVER_PACKAGE) == null) return
        try { context.sendBroadcast(Intent(PREFIX + "ACTION_STOP_SERVICE").setComponent(component)) }
        catch (e: Exception) { recordCommand("Receiver stop blocked: ${e.javaClass.simpleName}"); return }
        synchronized(lock) { queryId++; inFlight = false; observedAt = null; session = JSONObject() }
        recordCommand("Receiver stop requested")
        refresh(context, true)
    }

    private fun launch(activity: Activity, intent: Intent): Boolean =
        try { activity.startActivity(intent); true }
        catch (e: Exception) { recordCommand("Could not open receiver/setup: ${e.javaClass.simpleName}"); false }

    private fun recordCommand(text: String) = synchronized(lock) { commandStatus = text }
}
