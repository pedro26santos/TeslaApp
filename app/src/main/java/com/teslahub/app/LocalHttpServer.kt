package com.teslahub.app

import android.content.res.AssetManager
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LocalHttpServer(private val assets: AssetManager) {
    private data class Listener(val binding: LocalNetwork.Binding, val socket: ServerSocket)
    private val listeners = mutableMapOf<String, Listener>()
    private val handlers = Executors.newFixedThreadPool(4)
    @Volatile private var closed = false
    @Volatile var lastBindError: String? = null
        private set

    @Synchronized fun reconcile(found: List<LocalNetwork.Binding>) {
        if (closed) return
        lastBindError = null
        val keys = found.map { it.key }.toSet()
        for (key in listeners.keys.toList()) {
            if (key !in keys) listeners.remove(key)?.socket?.close()
        }
        for (binding in found) {
            if (binding.key in listeners) continue
            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress(binding.address, 8765), 8)
                listeners[binding.key] = Listener(binding, socket)
                thread(name = "TeslaHub-accept-${binding.key}", isDaemon = true) { acceptLoop(binding, socket) }
            } catch (e: Exception) {
                lastBindError = "Could not bind ${binding.key}: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    @Synchronized fun boundAddresses(): List<LocalNetwork.Binding> = listeners.values.map { it.binding }

    private fun acceptLoop(binding: LocalNetwork.Binding, serverSocket: ServerSocket) {
        while (!closed && !serverSocket.isClosed) {
            try {
                val client = serverSocket.accept()
                handlers.execute { handle(client, binding) }
            } catch (_: Exception) {
                if (!serverSocket.isClosed) HubRuntime.serverError = "Accept failed on ${binding.key}"
                break
            }
        }
    }

    private fun handle(client: Socket, binding: LocalNetwork.Binding) {
        client.use { socket ->
            try {
                socket.soTimeout = 5000
                val remote = socket.inetAddress as? Inet4Address
                if (remote == null || !LocalNetwork.sameSubnet(remote, binding)) {
                    respond(socket, 403, "text/plain", "Local subnet only".toByteArray())
                    return
                }
                val input = BufferedInputStream(socket.getInputStream())
                val line = readLine(input) ?: return
                val parts = line.split(' ')
                if (parts.size != 3 || parts[2] != "HTTP/1.1" && parts[2] != "HTTP/1.0") {
                    respond(socket, 400, "text/plain", "Bad request".toByteArray())
                    return
                }
                val method = parts[0]
                val path = parts[1].substringBefore('?')
                val headers = mutableMapOf<String, String>()
                var count = 0
                while (true) {
                    val header = readLine(input) ?: return
                    if (header.isEmpty()) break
                    count++
                    if (count > 32) return
                    val colon = header.indexOf(':')
                    if (colon > 0) headers[header.substring(0, colon).lowercase()] = header.substring(colon + 1).trim()
                }
                val host = headers["host"]?.lowercase()?.substringBefore(':')
                if (host != binding.key && host != "teslahub.local") {
                    respond(socket, 403, "text/plain", "Invalid host".toByteArray())
                    return
                }
                if (method == "GET" && path == "/api/state") {
                    respond(socket, 200, "application/json; charset=utf-8", HubRuntime.stateJson().toString().toByteArray(StandardCharsets.UTF_8))
                    return
                }
                if (method == "POST" && (path == "/api/report" || path == "/api/ping")) {
                    val origin = headers["origin"]
                    if (origin != null && origin != "http://${binding.key}:8765" && origin != "http://teslahub.local:8765") {
                        respond(socket, 403, "text/plain", "Invalid origin".toByteArray())
                        return
                    }
                    if (path == "/api/report") {
                        if (headers["content-type"]?.substringBefore(';') != "application/json") {
                            respond(socket, 415, "text/plain", "JSON required".toByteArray())
                            return
                        }
                        val size = headers["content-length"]?.toIntOrNull()
                        if (size == null || size !in 1..8192) {
                            respond(socket, 413, "text/plain", "Invalid report size".toByteArray())
                            return
                        }
                        val body = ByteArray(size)
                        var offset = 0
                        while (offset < size) {
                            val read = input.read(body, offset, size - offset)
                            if (read < 0) return
                            offset += read
                        }
                        HubRuntime.recordBrowser(JSONObject(String(body, StandardCharsets.UTF_8)))
                    } else HubRuntime.pingBrowser()
                    respond(socket, 200, "application/json; charset=utf-8", "{\"ok\":true}".toByteArray())
                    return
                }
                val files = mapOf(
                    "/" to Pair("web/index.html", "text/html; charset=utf-8"),
                    "/index.html" to Pair("web/index.html", "text/html; charset=utf-8"),
                    "/styles.css" to Pair("web/styles.css", "text/css; charset=utf-8"),
                    "/app.js" to Pair("web/app.js", "application/javascript; charset=utf-8")
                )
                val file = files[path]
                if (method == "GET" && file != null) {
                    respond(socket, 200, file.second, assets.open(file.first).use { it.readBytes() })
                } else respond(socket, 404, "text/plain", "Not found".toByteArray())
            } catch (e: Exception) {
                try { respond(socket, 500, "text/plain", "Server error".toByteArray()) } catch (_: Exception) { }
                HubRuntime.lastRequestError = "Request error: ${e.javaClass.simpleName}"
            }
        }
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = java.io.ByteArrayOutputStream()
        while (bytes.size() < 8192) {
            val b = input.read()
            if (b < 0) return null
            if (b == 10) return bytes.toString("ISO-8859-1").trimEnd('\r')
            bytes.write(b)
        }
        return null
    }

    private fun respond(socket: Socket, code: Int, contentType: String, body: ByteArray) {
        val reason = when (code) { 200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; 404 -> "Not Found"; 413 -> "Payload Too Large"; 415 -> "Unsupported Media Type"; else -> "Internal Server Error" }
        val headers = "HTTP/1.1 $code $reason\r\nContent-Type: $contentType\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'\r\nConnection: close\r\n\r\n"
        socket.getOutputStream().apply {
            write(headers.toByteArray(StandardCharsets.US_ASCII))
            write(body)
            flush()
        }
    }

    @Synchronized fun close() {
        closed = true
        listeners.values.forEach { try { it.socket.close() } catch (_: Exception) { } }
        listeners.clear()
        handlers.shutdownNow()
        try { handlers.awaitTermination(1, TimeUnit.SECONDS) } catch (_: InterruptedException) { }
    }
}
