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
- [ ] Capture the Tesla browser capability report and test provider navigation/playback separately.

**Current physical result, 12 October 2026:** Version **0.1.6** loads the launcher in the owner's Tesla Model 3 at `http://169.254.77.1:8765/`, with **Local browser connected** and all four principal buttons visible. The owner previously reported software **2026.32.7**. The OPPO and PC checks also passed. The launcher remains strictly local. The car connection gate has passed; browser capability measurements, provider playback, automation, and Android Auto projection remain separate work.

Version 0.1.6 includes the stale-constructor startup crash fix and stable status refresh. Both foreground services survived Home and reopening on the OPPO. Earlier RFC 1918 and shared-space routes failed; the diagnosis and successful link-local route are recorded in [ARCHITECTURE.md](ARCHITECTURE.md). The owner's instruction permits committing the fix now that the car test works.

## Phase 2 — connection automation

Bluetooth paired-device selection and connection state, disconnect grace period, settings persistence, and a supported hotspot strategy. Begin only after Phase 1's physical browser test.

## Phase 3 — real Android Auto receiver

Select a legally compatible receiver, retain required notices, and prove same-device projection on the OPPO. Record receiver connection, negotiated video settings, and audio route. Do not begin Tesla streaming until this is stable.

## Phase 4 — Tesla projection

Choose transport after Tesla capability results; implement aspect-preserving 720p/30 first, then 900p and 1080p. Map touch to receiver coordinates. Measure latency, CPU, temperature, and Bluetooth audio behavior. Treat 60 FPS as experimental.

## Phase 5 — reliability and polish

Auto display presets, performance settings, reconnect handling, log export, thermal tuning, and verified Theater behavior if technically available.
