package com.teslahub.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ServerHealthCheckTest {
    private val binding = LocalNetwork.Binding(InetAddress.getByName("127.0.0.1") as Inet4Address, 24, "test")

    @Test fun successfulCheckRequiresARealTeslaHubResponse() {
        assertTrue(probe("HTTP/1.1 200 OK", ServerHealthCheck.BODY).passed)
        assertFalse(probe("HTTP/1.1 200 OK", "Another service\n").passed)
        assertFalse(probe("HTTP/1.1 403 Forbidden", "Local subnet only\n").passed)
    }

    @Test fun missingListenerReportsConnectionFailure() {
        val port = ServerSocket(0, 1, binding.address).use { it.localPort }
        val result = ServerHealthCheck.check(binding, port)
        assertFalse(result.passed)
        assertTrue(result.detail.contains("ConnectException"))
    }

    private fun probe(status: String, body: String): ServerHealthCheck.Result {
        val executor = Executors.newSingleThreadExecutor()
        try {
            ServerSocket(0, 1, binding.address).use { server ->
                server.soTimeout = 2000
                val response = executor.submit {
                    server.accept().use { client ->
                        client.soTimeout = 2000
                        val input = client.getInputStream().bufferedReader(StandardCharsets.US_ASCII)
                        while (true) {
                            val line = input.readLine() ?: error("No health request")
                            if (line.isEmpty()) break
                        }
                        val reply = "$status\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                        client.getOutputStream().write(reply.toByteArray(StandardCharsets.US_ASCII))
                    }
                }
                val result = ServerHealthCheck.check(binding, server.localPort)
                response.get(3, TimeUnit.SECONDS)
                return result
            }
        } finally { executor.shutdownNow() }
    }
}
