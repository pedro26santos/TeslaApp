package com.teslahub.app

import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

object ServerHealthCheck {
    const val BODY = "Tesla Hub OK\n"

    data class Result(val binding: LocalNetwork.Binding, val passed: Boolean, val detail: String)

    fun check(binding: LocalNetwork.Binding, port: Int = 8765): Result {
        return try {
            Socket().use { socket ->
                // Keep this check local even when the phone's default network is cellular.
                socket.bind(InetSocketAddress(binding.address, 0))
                socket.soTimeout = 1500
                socket.connect(InetSocketAddress(binding.address, port), 1500)
                val request = "GET /api/health HTTP/1.1\r\nHost: ${binding.key}:$port\r\nConnection: close\r\n\r\n"
                socket.getOutputStream().apply {
                    write(request.toByteArray(StandardCharsets.US_ASCII))
                    flush()
                }
                val input = socket.getInputStream().bufferedReader(StandardCharsets.US_ASCII)
                val status = input.readLine().orEmpty()
                var headers = 0
                while (true) {
                    val header = input.readLine() ?: error("Incomplete HTTP response")
                    if (header.isEmpty()) break
                    check(++headers <= 32) { "Too many HTTP headers" }
                }
                val passed = status == "HTTP/1.1 200 OK" && input.readLine() == BODY.trimEnd()
                Result(binding, passed, if (passed) "Passed on phone" else "Unexpected response: ${status.take(100)}")
            }
        } catch (e: Exception) {
            Result(binding, false, "${e.javaClass.simpleName}: ${e.message.orEmpty().take(200)}")
        }
    }
}
