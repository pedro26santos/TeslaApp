package com.teslahub.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address

object NetworkDiagnostics {
    data class WifiLink(val interfaceName: String, val addresses: List<String>, val gateways: List<String>)
    data class Snapshot(val wifiLinks: List<WifiLink> = emptyList(), val vpnActive: Boolean = false,
        val checked: Boolean = false) {
        val wifiInterfaces: Set<String> get() = wifiLinks.map { it.interfaceName }.toSet()
    }

    fun read(context: Context): Snapshot {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val wifi = mutableListOf<WifiLink>()
        var vpn = false
        for (network in manager.allNetworks) {
            val capabilities = manager.getNetworkCapabilities(network) ?: continue
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                vpn = true
                continue
            }
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val link = manager.getLinkProperties(network) ?: continue
            val name = link.interfaceName ?: continue
            val addresses = link.linkAddresses.mapNotNull {
                val ip = it.address as? Inet4Address
                if (ip != null && LocalNetwork.isPrivate(ip)) "${ip.hostAddress}/${it.prefixLength}" else null
            }
            val gateways = link.routes.mapNotNull {
                val ip = it.gateway as? Inet4Address
                if (ip != null && LocalNetwork.isPrivate(ip)) ip.hostAddress else null
            }.distinct()
            wifi.add(WifiLink(name, addresses, gateways))
        }
        return Snapshot(wifi, vpn, checked = true)
    }
}
