package com.teslahub.app

import android.content.Context
import android.content.Intent
import android.net.VpnService

object NetworkControl {
    const val AUTOMATIC = "com.teslahub.app.AUTOMATIC_START"
    const val STOP_LOCAL_ROUTE = "com.teslahub.app.STOP_LOCAL_ROUTE"

    fun startAutomatic(context: Context): String? {
        val errors = mutableListOf<String>()
        if (!HubRuntime.serviceRunning) {
            try { context.startForegroundService(Intent(context, HubService::class.java).setAction(AUTOMATIC)) }
            catch (e: Exception) { errors.add("Server start blocked: ${e.javaClass.simpleName}; open Tesla Hub") }
        }
        if (!HubRuntime.localRouteRunning) {
            if (VpnService.prepare(context) != null) {
                errors.add("Tesla local connection needs VPN consent; open Settings in Tesla Hub")
            } else {
                try { context.startForegroundService(Intent(context, LocalRouteService::class.java).setAction(AUTOMATIC)) }
                catch (e: Exception) { errors.add("Local connection start blocked: ${e.javaClass.simpleName}; open Tesla Hub") }
            }
        }
        return errors.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    fun stopAutomatic(context: Context) {
        if (HubRuntime.localRouteAutomatic) stopLocalRoute(context)
        if (HubRuntime.serverAutomatic) context.stopService(Intent(context, HubService::class.java))
    }

    fun stopLocalRoute(context: Context) {
        // Android binds to an established VpnService. stopService alone leaves it bound,
        // so the service must close its tunnel before Android can destroy it.
        try {
            context.startService(Intent(context, LocalRouteService::class.java).setAction(STOP_LOCAL_ROUTE))
        } catch (e: Exception) {
            HubRuntime.automationLastError = "Local connection stop blocked: ${e.javaClass.simpleName}; open Tesla Hub and stop it"
        }
    }

    fun disableAutomation(context: Context) {
        HubPreferences(context).automationEnabled = false
        context.stopService(Intent(context, AutomationService::class.java))
        stopAutomatic(context)
        HubRuntime.automationStatus = "Disabled"
    }

    const val CONNECTED_EVENT = "selected_car_connected_event"

    fun resumeMonitoring(context: Context, connectedEvent: Boolean = false): String? = try {
        context.startForegroundService(Intent(context, AutomationService::class.java).putExtra(CONNECTED_EVENT, connectedEvent))
        null
    } catch (e: Exception) {
        "Connection monitor could not start: ${e.javaClass.simpleName}; open Tesla Hub to resume".also {
            HubRuntime.automationLastError = it
            HubRuntime.automationStatus = "Needs attention"
        }
    }
}
