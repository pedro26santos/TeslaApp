package com.teslahub.app

import android.content.Context
import android.content.Intent
import android.provider.Settings

object HotspotAssistance {
    // The OPPO is API 34. The SDK's public TetheringManager API is API 36; older normal
    // apps cannot control internet-sharing tethering with its privileged system APIs.
    // LocalOnlyHotspot provides no upstream internet and does not meet this project's needs.
    fun settingsIntent(context: Context): Intent {
        val direct = Intent("android.settings.TETHER_SETTINGS")
        return if (direct.resolveActivity(context.packageManager) != null) direct
        else Intent(Settings.ACTION_WIRELESS_SETTINGS)
    }

    fun needsActivation(context: Context): Boolean {
        val connected = NetworkDiagnostics.read(context).wifiInterfaces
        val found = LocalNetwork.discover(connected).any { binding ->
            val name = binding.interfaceName.lowercase()
            !binding.connectedWifi && listOf("wlan", "swlan", "ap", "wifi").any { name.startsWith(it) }
        }
        HubRuntime.hotspotStrategy = if (found)
            "Hotspot interface detected; control remains manual"
        else "Enable Personal Hotspot in phone settings"
        return !found
    }
}
