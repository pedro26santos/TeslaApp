package com.teslahub.app

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetwork {
    data class Binding(val address: Inet4Address, val prefixLength: Int, val interfaceName: String,
        val connectedWifi: Boolean = false) {
        val url: String get() = "http://${address.hostAddress}:8765/"
        val key: String get() = address.hostAddress ?: ""
    }

    fun discover(connectedWifiInterfaces: Set<String> = emptySet()): List<Binding> {
        val result = mutableListOf<Binding>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
        while (interfaces.hasMoreElements()) {
            val nic = interfaces.nextElement()
            if (!nic.isUp || nic.isLoopback) continue
            val name = nic.name.lowercase()
            // Exclude cellular and VPN interfaces; listen only on Wi-Fi, hotspot, or wired LAN.
            if (!listOf("wlan", "swlan", "ap", "wifi", "eth", "rndis", "usb", "br").any { name.startsWith(it) }) continue
            for (item in nic.interfaceAddresses) {
                val ip = item.address as? Inet4Address ?: continue
                if (!isPrivate(ip)) continue
                result.add(Binding(ip, item.networkPrefixLength.toInt(), nic.name, nic.name in connectedWifiInterfaces))
            }
        }
        return preferredOrder(result.distinctBy { it.key })
    }

    fun preferredOrder(bindings: List<Binding>): List<Binding> = bindings.sortedWith(compareBy<Binding> {
        when {
            it.connectedWifi -> 2
            it.interfaceName.lowercase().startsWith("ap") || it.interfaceName.lowercase().startsWith("swlan") -> 0
            else -> 1
        }
    }.thenBy { it.interfaceName })

    fun isPrivate(ip: Inet4Address): Boolean {
        val bytes = ip.address.map { it.toInt() and 0xff }
        return bytes[0] == 10 ||
            (bytes[0] == 172 && bytes[1] in 16..31) ||
            (bytes[0] == 192 && bytes[1] == 168)
    }

    fun sameSubnet(remote: Inet4Address, binding: Binding): Boolean {
        val prefix = binding.prefixLength
        if (prefix !in 8..32 || !isPrivate(remote)) return false
        val a = remote.address
        val b = binding.address.address
        for (bit in 0 until prefix) {
            val byteIndex = bit / 8
            val mask = 1 shl (7 - bit % 8)
            if ((a[byteIndex].toInt() and mask) != (b[byteIndex].toInt() and mask)) return false
        }
        return true
    }
}
