package com.teslahub.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.teslahub.app.ConnectionSession.Effect
import com.teslahub.app.ConnectionSession.Observation

class ConnectionSessionTest {
    private val grace = 60_000L

    @Test fun anInitiallyDisconnectedCarDoesNotStartOrScheduleNetworking() {
        val session = ConnectionSession()
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 1000, grace))
        assertNull(session.remainingSeconds(1000, grace))
        assertEquals(Effect.NONE, session.update(Observation.UNKNOWN, 70_000, grace))
    }

    @Test fun connectionStartsOnceAndDuplicateEventsDoNotRestartTheSession() {
        val session = ConnectionSession()
        assertEquals(Effect.START, session.update(Observation.CONNECTED, 1000, grace))
        assertEquals(Effect.NONE, session.update(Observation.CONNECTED, 2000, grace))
        assertNull(session.remainingSeconds(2000, grace))
    }

    @Test fun duplicateDisconnectsKeepTheOriginalDeadlineAndStopOnlyOnce() {
        val session = ConnectionSession()
        session.update(Observation.CONNECTED, 0, grace)
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 1000, grace))
        assertEquals(60, session.remainingSeconds(1000, grace))
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 30_000, grace))
        assertEquals(31, session.remainingSeconds(30_000, grace))
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 60_999, grace))
        assertEquals(Effect.STOP, session.update(Observation.DISCONNECTED, 61_000, grace))
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 90_000, grace))
        assertNull(session.remainingSeconds(90_000, grace))
    }

    @Test fun reconnectBeforeTheDeadlineCancelsShutdown() {
        val session = ConnectionSession()
        session.update(Observation.CONNECTED, 0, grace)
        session.update(Observation.DISCONNECTED, 1000, grace)
        assertEquals(Effect.NONE, session.update(Observation.CONNECTED, 50_000, grace))
        assertNull(session.remainingSeconds(61_000, grace))
        assertEquals(Effect.NONE, session.update(Observation.CONNECTED, 61_000, grace))
        session.update(Observation.DISCONNECTED, 70_000, grace)
        assertEquals(Effect.NONE, session.update(Observation.DISCONNECTED, 120_000, grace))
        assertEquals(Effect.STOP, session.update(Observation.DISCONNECTED, 130_000, grace))
    }

    @Test fun aNewConnectionAfterShutdownStartsANewSession() {
        val session = ConnectionSession()
        session.update(Observation.CONNECTED, 0, grace)
        session.update(Observation.DISCONNECTED, 1000, grace)
        session.update(Observation.DISCONNECTED, 61_000, grace)
        assertEquals(Effect.START, session.update(Observation.CONNECTED, 70_000, grace))
    }

    @Test fun changingTheDelayUsesElapsedDisconnectTime() {
        val session = ConnectionSession()
        session.update(Observation.CONNECTED, 0, grace)
        session.update(Observation.DISCONNECTED, 1000, grace)
        assertEquals(Effect.STOP, session.update(Observation.DISCONNECTED, 11_000, 10_000))
    }

    @Test fun unknownStateDoesNotInventADisconnectButDoesNotCancelAConfirmedOne() {
        val session = ConnectionSession()
        session.update(Observation.CONNECTED, 0, grace)
        assertEquals(Effect.NONE, session.update(Observation.UNKNOWN, 100_000, grace))
        assertNull(session.remainingSeconds(100_000, grace))
        session.update(Observation.DISCONNECTED, 101_000, grace)
        assertEquals(Effect.STOP, session.update(Observation.UNKNOWN, 161_000, grace))
    }
}
