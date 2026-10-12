package com.teslahub.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class LocalHttpServerTest {
    private val binding = LocalNetwork.Binding(InetAddress.getByName("127.0.0.1") as Inet4Address, 24, "test")

    @Test fun failedAcceptClosesTheListenerAndAllowsRebinding() {
        val attempts = AtomicInteger()
        val errors = CopyOnWriteArrayList<String>()
        val sockets = CopyOnWriteArrayList<ServerSocket>()
        val server = LocalHttpServer(
            readAsset = { byteArrayOf() }, onExternalConnection = {}, onRequestError = {},
            onListenerError = { errors.add(it) }, port = 0,
            socketFactory = {
                val socket = if (attempts.getAndIncrement() == 0) object : ServerSocket() {
                    override fun accept(): Socket = throw SocketException("Injected accept failure")
                } else ServerSocket()
                sockets.add(socket)
                socket
            }
        )
        try {
            server.reconcile(listOf(binding))
            await { errors.isNotEmpty() && server.boundAddresses().isEmpty() }
            assertTrue(errors.single().contains("Injected accept failure"))
            assertTrue(sockets.first().isClosed)

            server.reconcile(listOf(binding))
            assertEquals(listOf(binding), server.boundAddresses())
            assertEquals(2, attempts.get())
        } finally { server.close() }
    }

    @Test fun subnetChangeReplacesTheListenerEvenWhenTheIpIsUnchanged() {
        val sockets = CopyOnWriteArrayList<ServerSocket>()
        val server = LocalHttpServer(
            readAsset = { byteArrayOf() }, onExternalConnection = {}, onRequestError = {}, onListenerError = {},
            port = 0, socketFactory = { ServerSocket().also { sockets.add(it) } }
        )
        try {
            server.reconcile(listOf(binding))
            val changed = binding.copy(prefixLength = 16)
            server.reconcile(listOf(changed))
            assertTrue(sockets.first().isClosed)
            assertEquals(listOf(changed), server.boundAddresses())
            assertEquals(2, sockets.size)
            server.reconcile(emptyList())
            assertTrue(server.boundAddresses().isEmpty())
            assertTrue(sockets.last().isClosed)
        } finally { server.close() }
    }

    @Test fun healthEndpointRespondsThroughTheConfiguredClientPolicy() {
        val sockets = CopyOnWriteArrayList<ServerSocket>()
        val errors = CopyOnWriteArrayList<String>()
        val server = LocalHttpServer(
            readAsset = { byteArrayOf() }, onExternalConnection = {}, onRequestError = { errors.add(it) },
            onListenerError = { errors.add(it) }, port = 0,
            socketFactory = { ServerSocket().also { sockets.add(it) } },
            allowClient = { remote, _ -> remote == binding.address }
        )
        try {
            server.reconcile(listOf(binding))
            val result = ServerHealthCheck.check(binding, sockets.single().localPort)
            assertTrue(result.detail, result.passed)
            assertTrue(errors.toString(), errors.isEmpty())
        } finally { server.close() }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 2_000_000_000
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue("Listener did not recover within two seconds", condition())
    }
}
