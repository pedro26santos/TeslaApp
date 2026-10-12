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
- [ ] Confirm behavior on OPPO Find X3 Pro and Tesla Model 3 using [TESTING.md](TESTING.md).

## Phase 2 — connection automation

Bluetooth paired-device selection and connection state, disconnect grace period, settings persistence, and a supported hotspot strategy. Begin only after Phase 1's physical browser test.

## Phase 3 — real Android Auto receiver

Select a legally compatible receiver, retain required notices, and prove same-device projection on the OPPO. Record receiver connection, negotiated video settings, and audio route. Do not begin Tesla streaming until this is stable.

## Phase 4 — Tesla projection

Choose transport after Tesla capability results; implement aspect-preserving 720p/30 first, then 900p and 1080p. Map touch to receiver coordinates. Measure latency, CPU, temperature, and Bluetooth audio behavior. Treat 60 FPS as experimental.

## Phase 5 — reliability and polish

Auto display presets, performance settings, reconnect handling, log export, thermal tuning, and verified Theater behavior if technically available.
