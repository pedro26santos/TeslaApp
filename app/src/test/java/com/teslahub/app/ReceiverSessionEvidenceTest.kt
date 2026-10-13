package com.teslahub.app

import org.junit.Assert.*
import org.junit.Test

class ReceiverSessionEvidenceTest {
    @Test fun handshakeAndConfiguredTransportDoNotProveVideo() {
        assertFalse(ReceiverSessionEvidence.videoDecoded(true, false, 0))
        assertFalse(ReceiverSessionEvidence.videoDecoded(true, true, 0))
        assertTrue(ReceiverSessionEvidence.videoDecoded(true, true, 1))
    }

    @Test fun framesFromDisconnectedSessionCannotProveVideo() {
        assertFalse(ReceiverSessionEvidence.videoDecoded(false, true, 123))
        assertFalse(ReceiverSessionEvidence.videoDecoded(true, false, 123))
        assertFalse(ReceiverSessionEvidence.videoDecoded(true, true, -1))
    }

    @Test fun missingAndExpiredSnapshotsCannotReportActiveSession() {
        assertFalse(ReceiverSessionEvidence.fresh(null, 20_000))
        assertTrue(ReceiverSessionEvidence.fresh(1000, 15_999))
        assertFalse(ReceiverSessionEvidence.fresh(1000, 16_000))
        assertFalse(ReceiverSessionEvidence.fresh(20_000, 1000))
    }

    @Test fun legacyAndUnknownVersionsCannotStartVpnChangingSelfMode() {
        assertFalse(ReceiverSessionEvidence.supportsServerSelfMode(null))
        assertFalse(ReceiverSessionEvidence.supportsServerSelfMode("unknown"))
        assertFalse(ReceiverSessionEvidence.supportsServerSelfMode("17.3.123-release"))
        assertTrue(ReceiverSessionEvidence.supportsServerSelfMode("17.4.1"))
        assertTrue(ReceiverSessionEvidence.supportsServerSelfMode("17.7.663654-release"))
    }
}
