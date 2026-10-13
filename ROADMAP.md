# Roadmap

## Phase 0 — analysis

- [x] Inspect local workspace and toolchain; the supplied directory was empty and had no Git metadata.
- [x] Research receiver options, hotspot constraints, mDNS, streaming browser APIs, and licenses.
- [x] Document architecture and hardware unknowns.

## Phase 1 — foundation

- [x] Kotlin Android app, manual foreground HTTP server, local IP status, numeric fallback, NSD attempt.
- [x] Tesla launcher with all four principal buttons, Settings, and Diagnostics.
- [x] Browser viewport, user agent, media and storage probes, Vodafone compatibility diagnostics.
- [x] Direct Stremio, RTP Play, and Vodafone URL navigation with configurable destinations.
- [x] Confirm launcher connectivity on OPPO Find X3 Pro, PC, and Tesla Model 3 using [TESTING.md](TESTING.md).
- [x] Record Tesla browser capability observations and both viewport modes from the owner's diagnostic photos in [TESLA_BROWSER_REPORT.md](TESLA_BROWSER_REPORT.md).
- [ ] Obtain the exported browser report and test provider navigation/playback separately.

**Current physical result, 12 October 2026:** Version **0.1.6** loads the launcher in the owner's Tesla Model 3 at `http://169.254.77.1:8765/`, with **Local browser connected** and all four principal buttons visible. The owner previously reported software **2026.32.7**. The OPPO and PC checks also passed. The launcher remains strictly local. The car connection gate has passed; browser capability measurements, provider playback, automation, and Android Auto projection remain separate work.

Version 0.1.6 includes the stale-constructor startup crash fix and stable status refresh. Both foreground services survived Home and reopening on the OPPO. Earlier RFC 1918 and shared-space routes failed; the diagnosis and successful link-local route are recorded in [ARCHITECTURE.md](ARCHITECTURE.md). The owner's instruction permits committing the fix now that the car test works.

## Phase 2 — connection automation

Bluetooth paired-device selection and connection state, disconnect grace period, settings persistence, and a supported hotspot strategy. Begin only after Phase 1's physical browser test.

Version **0.2.1** implements:

- [x] Paired phone/audio Tesla selector and Android Nearby Devices permission flow.
- [x] Foreground monitor using selected-device ACL events and phone/audio profile snapshots.
- [x] Automatic startup of the server and the verified link-local address after prior VPN consent.
- [x] Configurable 10–600 second disconnect delay, default 60; reconnect cancels shutdown.
- [x] Settings persistence and explicit automatic/manual service control.
- [x] User-assisted Personal Hotspot activation with a settings notification shortcut.
- [x] Opt-in boot/package-replacement recovery and surfaced background-start failures.
- [x] Owner confirms automatic startup on Tesla Bluetooth connection and automatic shutdown after disconnect in APK 0.2.1.
- [x] Verify manual startup/stop/restart, app reopening, and PC access over the OPPO hotspot by USB checks.
- [ ] Individually verify reconnect cancellation, changed-delay persistence, locked-screen timing, reboot recovery, manual ownership, and audio/call behavior using [TESTING_PHASE2.md](TESTING_PHASE2.md).
- [ ] Investigate supported automatic internet-sharing hotspot control on this Android 14 OPPO; retain manual activation when unavailable.

**Physical result, 13 October 2026:** The owner reports APK **0.2.1** working as expected, with automatic server startup on Bluetooth connection and shutdown after disconnect. The diagnostic photos show the selected Tesla connected, an active monitor, the 60-second delay, no automation error, and successful local browser access. This verifies the core connection automation milestone and permits its commit under the owner's instruction. The remaining reliability checks above are not claimed as tested. The observations and implementation constraints are recorded in [TESLA_BROWSER_REPORT.md](TESLA_BROWSER_REPORT.md).

## Phase 3 — real Android Auto receiver

Select a legally compatible receiver, retain required notices, and prove same-device projection on the OPPO. Record receiver connection, negotiated video settings, and audio route. Do not begin Tesla streaming until this is stable.

Current local build: **Tesla Hub 0.3.0** with a separate source-built Open Headunit receiver prototype, **3.5.0-beta4-teslahub1**, pinned at source revision `4ee1d58f6cf7f39f7dc0a6ef93509553401ec6aa` plus the recorded AGPL patch.

- [x] Inspect source, public command/state interface, same-device startup, build dependencies and AGPL notices.
- [x] Build the receiver for arm64 and install it separately as `com.teslahub.receiver` on the OPPO.
- [x] Add native receiver start/stop/setup controls and bounded explicit-query diagnostics to Tesla Hub.
- [x] Distinguish requested/announced settings, transport connection, and actual rendered frames; expose phone video metadata in browser diagnostics.
- [x] Build Tesla Hub 0.3.0, pass all 22 unit tests, and install the update on the OPPO.
- [x] Visually inspect real Android Auto/Waze on the physical OPPO and confirm actual 1280 × 720 H.264 decoded frames.
- [x] Verify touch opens Android Auto's application drawer and native diagnostics expose fresh frame evidence.
- [x] Verify receiver stop clears video evidence and reconnect returns actual decoded frames.
- [x] Verify PC HTTP access through Tesla Hub's local VPN while the receiver runs, and after receiver stop.
- [x] Owner accepts the fresh parked-car checklist as working; photos independently confirm Tesla browser access alongside live receiver decoding with no receiver error.
- [ ] Record individually itemized media/navigation speaker routes, hands-free call behavior and exact shutdown timing during the next projection test.

The OPPO has Android Auto **17.7.663654-release**. On **13 October 2026**, the owner completed setup and the agent inspected real Waze projection on the connected physical OPPO. The qualification profile is 1280 × 720, 200 DPI, H.264 and advertised 30 FPS; short renderer windows measured approximately 28–29 FPS. Two sessions, real touch, truthful disconnected diagnostics, and PC access through the active local VPN passed. The owner subsequently reported the fresh parked-car test working as expected. Tesla photos show growing rendered frames (2,588 → 2,859), fresh decoded video, local browser access and no receiver error. This accepts the Phase 3 milestone and permits its commit. Detailed audio/call/timing observations remain listed separately above; browser projection is Phase 4 work.

## Phase 4 — Tesla projection

Choose transport after Tesla capability results; implement aspect-preserving 720p/30 first, then 900p and 1080p. Map touch to receiver coordinates. Measure latency, CPU, temperature, and Bluetooth audio behavior. Treat 60 FPS as experimental.

## Phase 5 — reliability and polish

Auto display presets, performance settings, reconnect handling, log export, thermal tuning, and verified Theater behavior if technically available.
