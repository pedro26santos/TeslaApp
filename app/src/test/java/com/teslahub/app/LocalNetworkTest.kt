package com.teslahub.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class LocalNetworkTest {
    private fun ip(value: String) = InetAddress.getByName(value) as Inet4Address

    @Test fun privateAddressRangesExcludePublicAndCarrierSpace() {
        assertTrue(LocalNetwork.isPrivate(ip("192.168.43.1")))
        assertTrue(LocalNetwork.isPrivate(ip("172.20.1.1")))
        assertTrue(LocalNetwork.isPrivate(ip("10.0.0.1")))
        assertFalse(LocalNetwork.isPrivate(ip("100.64.0.1")))
        assertFalse(LocalNetwork.isPrivate(ip("8.8.8.8")))
    }

    @Test fun onlyTheBoundSubnetMayReachTheServer() {
        val binding = LocalNetwork.Binding(ip("192.168.43.1"), 24, "ap0")
        assertTrue(LocalNetwork.sameSubnet(ip("192.168.43.20"), binding))
        assertFalse(LocalNetwork.sameSubnet(ip("192.168.44.20"), binding))
        assertFalse(LocalNetwork.sameSubnet(ip("8.8.8.8"), binding))
    }

    @Test fun hotspotCandidateComesBeforeThePhonesConnectedWifi() {
        val upstream = LocalNetwork.Binding(ip("10.0.0.20"), 24, "wlan0", connectedWifi = true)
        val hotspot = LocalNetwork.Binding(ip("192.168.43.1"), 24, "wlan2")
        assertEquals(listOf(hotspot, upstream), LocalNetwork.preferredOrder(listOf(upstream, hotspot)))
    }
}
