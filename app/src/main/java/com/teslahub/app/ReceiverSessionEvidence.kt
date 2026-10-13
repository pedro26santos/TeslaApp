package com.teslahub.app

/** A TCP/handshake connection alone is not evidence of Android Auto video. */
object ReceiverSessionEvidence {
    const val FRESH_MILLIS = 15_000L

    fun videoDecoded(connected: Boolean, transportStarted: Boolean, frames: Long): Boolean =
        connected && transportStarted && frames > 0

    fun fresh(observedAt: Long?, now: Long): Boolean =
        observedAt != null && now >= observedAt && now - observedAt < FRESH_MILLIS

    fun supportsServerSelfMode(version: String?): Boolean {
        val match = Regex("^(\\d+)\\.(\\d+)").find(version.orEmpty()) ?: return false
        val major = match.groupValues[1].toIntOrNull() ?: return false
        val minor = match.groupValues[2].toIntOrNull() ?: return false
        return major > 17 || major == 17 && minor >= 4
    }
}
