package com.teslahub.app

import java.net.Inet4Address
import java.net.InetAddress

object LocalRoutePolicy {
    // Link-local address for an intentionally local server. Android excludes link-local
    // addresses from its VPN-address ingress discard rules. Keep normal OS filtering intact.
    const val ADDRESS = "169.254.77.1"
    val binding: LocalNetwork.Binding
        get() = LocalNetwork.Binding(InetAddress.getByName(ADDRESS) as Inet4Address, 32, "TeslaHub local route")

    fun permits(remote: Inet4Address, physicalBindings: List<LocalNetwork.Binding>): Boolean =
        remote.hostAddress == ADDRESS || physicalBindings.any { LocalNetwork.sameSubnet(remote, it) }

    fun packetSummary(bytes: ByteArray, size: Int): String {
        if (size <= 0 || size > bytes.size) return "Invalid packet length: $size"
        val version = bytes[0].toInt() ushr 4 and 15
        return when {
            version == 6 && size >= 40 -> "IPv6: next header=${bytes[6].toInt() and 255}, bytes=$size (not IPv4 HTTP)"
            version == 4 && size >= 20 -> {
                val header = (bytes[0].toInt() and 15) * 4
                if (header !in 20..60 || header > size) return "Invalid IPv4 header, bytes=$size"
                val source = bytes.copyOfRange(12, 16).joinToString(".") { (it.toInt() and 255).toString() }
                val destination = bytes.copyOfRange(16, 20).joinToString(".") { (it.toInt() and 255).toString() }
                "IPv4: $source -> $destination, protocol=${bytes[9].toInt() and 255}, bytes=$size"
            }
            else -> "Unrecognized packet: version=$version, bytes=$size"
        }
    }
}
