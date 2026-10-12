package com.teslahub.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class LocalRoutePolicyTest {
    private fun ip(value: String) = InetAddress.getByName(value) as Inet4Address
    private val hotspot = LocalNetwork.Binding(ip("10.5.6.1"), 24, "wlan2")

    @Test fun virtualListenerAllowsOnlyItsOwnProbeAndTheActualLocalSubnet() {
        assertTrue(LocalRoutePolicy.permits(ip(LocalRoutePolicy.ADDRESS), emptyList()))
        assertTrue(LocalRoutePolicy.permits(ip("10.5.6.40"), listOf(hotspot)))
        assertFalse(LocalRoutePolicy.permits(ip("10.5.7.40"), listOf(hotspot)))
        assertFalse(LocalRoutePolicy.permits(ip("192.168.9.40"), listOf(hotspot)))
        assertFalse(LocalRoutePolicy.permits(ip("100.127.255.2"), listOf(hotspot)))
        assertFalse(LocalRoutePolicy.permits(ip("8.8.8.8"), listOf(hotspot)))
    }

    @Test fun virtualRouteDoesNotMakeCarrierAddressesOrdinaryLanBindings() {
        assertFalse(LocalNetwork.isPrivate(ip(LocalRoutePolicy.ADDRESS)))
        assertFalse(LocalNetwork.isPrivate(ip("100.127.255.1")))
        assertFalse(LocalRoutePolicy.permits(ip("10.5.6.40"), emptyList()))
    }

    @Test fun ipv6InterfaceTrafficIsNotMisidentifiedAsAnIpv4HttpRequest() {
        val packet = ByteArray(48).apply { this[0] = 0x60; this[6] = 58 }
        val summary = LocalRoutePolicy.packetSummary(packet.copyOf(1600), packet.size)
        assertTrue(summary.contains("IPv6"))
        assertTrue(summary.contains("next header=58"))
        assertTrue(summary.contains("not IPv4 HTTP"))
        assertTrue(LocalRoutePolicy.packetSummary(byteArrayOf(), 0).contains("Invalid"))
        assertTrue(LocalRoutePolicy.packetSummary(packet, 1600).contains("Invalid"))
    }
}
