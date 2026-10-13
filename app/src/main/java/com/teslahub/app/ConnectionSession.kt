package com.teslahub.app

/** Monotonic disconnect timing; duplicate observations never restart the grace period. */
class ConnectionSession {
    enum class Observation { CONNECTED, DISCONNECTED, UNKNOWN }
    enum class Effect { NONE, START, STOP }
    private var active = false
    private var disconnectedAt: Long? = null

    fun update(observation: Observation, now: Long, graceMillis: Long): Effect {
        require(graceMillis >= 0)
        if (observation == Observation.CONNECTED) {
            disconnectedAt = null
            if (!active) { active = true; return Effect.START }
            return Effect.NONE
        }
        if (observation == Observation.DISCONNECTED && active && disconnectedAt == null) disconnectedAt = now
        val start = disconnectedAt
        if (active && start != null && now - start >= graceMillis) {
            active = false
            disconnectedAt = null
            return Effect.STOP
        }
        return Effect.NONE
    }

    fun remainingSeconds(now: Long, graceMillis: Long): Int? = disconnectedAt?.let {
        ((graceMillis - (now - it)).coerceAtLeast(0) + 999).div(1000).toInt()
    }
}
