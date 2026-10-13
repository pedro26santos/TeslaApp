# Physical testing — Phase 3 / Tesla Hub 0.3.0

This phase proves a **real Android Auto receiver session on the OPPO**. The receiver is the separate, source-built **Tesla Hub Receiver (Open Headunit)** app, package `com.teslahub.receiver`. Tesla Hub's Android Auto browser button displays diagnostics; browser video and touch are Phase 4 work.

The OPPO inspection on 13 October 2026 found Android Auto **17.7.663654-release** and Android 14. The owner started Android Auto's developer headunit server during setup. Its server must still be running at the time of each connection attempt.

## Recorded OPPO and PC results — 13 October 2026

- Tesla Hub **0.3.0** and companion **3.5.0-beta4-teslahub1** are installed on the owner's OPPO CPH2173.
- Real Android Auto/Waze video was visually inspected on the physical OPPO. The receiver reported actual decoded **1280 × 720 H.264/AVC**, **200 DPI** and an advertised **30 FPS**. Short renderer log windows measured approximately **28–29 FPS**; this is not a sustained performance guarantee.
- Touch opened Android Auto's real application drawer. Native Tesla Hub diagnostics independently showed fresh transport/frame evidence.
- Stop cleared the active video evidence and showed **Disconnected**. Starting again returned real decoded frames; the second session grew beyond **13,000** rendered frames.
- While that receiver session was running, this PC reached **http://169.254.77.1:8765/api/health** and received **Tesla Hub OK**. Its state reply showed the local VPN active and fresh, connected receiver video. Stopping the receiver left the local health request working.
- The bench session was stopped and the owner's existing automatic Tesla connection setting restored. Hotspot settings were not changed.

## Owner's parked-car acceptance — 13 October 2026

After receiving the fresh-start, local-browser, receiver, audio/call and disconnect test checklist, the owner reported that everything worked as expected. The supplied Tesla-browser photos independently confirm:

- Fresh receiver query and actual decoded **1280 × 720 H.264/AVC** video on the OPPO.
- Rendered-frame count increasing from **2,588 to 2,859**, with last-frame ages **6 ms** and **23 ms**.
- Announced **200 DPI** and **30 FPS**; the photos do not measure frame rate.
- Receiver version **3.5.0-beta4-teslahub1**, Android Auto **17.7.663654-release**, and receiver error **None**.
- Tesla browser access to the phone while the Android Auto receiver runs. Browser projection remains explicitly pending in this version.

This owner report accepts the Phase 3 qualification and permits its commit under the instruction to commit only after working physical tests. Individual media/navigation speaker routes, call behavior and exact shutdown timing were not separately itemized; retain those observations in the next projection test. Configured **5: PCM** is a sink setting, not independent evidence of audible playback. No photos, private network addresses or account data are committed.

## 1. Install both APKs

The agent can do this while the unlocked phone is connected by USB. Existing Tesla Hub settings remain saved with an ordinary update.

1. Update Tesla Hub using `adb install -r app/build/outputs/apk/debug/app-debug.apk`; confirm app version **0.3.0**.
2. Install the companion APK with `adb install -r -t .tooling/teslahub-receiver-debug.apk`. It is an Android test APK and requires `-t`. The build script exports it from the receiver's Gradle APK intermediates.
3. Open Tesla Hub and verify the existing connection settings and automatic mode remain as configured. Do not uninstall either app as a troubleshooting step.

**Expected:** Both apps open without a crash. Tesla Hub's new **Android Auto receiver (phone test)** screen detects the companion and Android Auto versions.

**If it fails:** Leave USB connected and send the exact install/startup error. Do not proceed to car testing with a startup crash.

## 2. Start Android Auto's developer server — OPPO only

1. In Tesla Hub, open **Android Auto receiver (phone test) → Open Android Auto settings**.
2. If Developer settings is not available, scroll to **Version**, tap it **10 times**, and accept the confirmation.
3. Open the three-dot menu and choose **Start headunit server**. Leave its Android Auto notification active. This is the documented development-server path; no root, wireless receiver re-enabling, or Google component modification is used.
4. Return to Tesla Hub. Avoid another AA head unit/DHU connecting to this phone during the test.

**Expected:** Android Auto's headunit server remains running and available for the same-device receiver. If the server was already started, continue without starting a second session.

**If it fails:** Send the exact Android Auto settings screen or error and whether its server notification is visible. An accepted TCP socket alone is not a successful receiver test.

## 3. Complete receiver setup — OPPO only

1. Open **Open receiver setup** from Tesla Hub's receiver screen.
2. Read and accept the receiver's testing notice only while stationary. At **Como se liga?**, choose **MODO PRÓPRIO** (Self Mode), then **Seguinte**. The observed Android Auto 17.7 startup uses its headunit server on localhost.
3. Use these values in the remaining wizard pages, tapping **Seguinte** after each:

   | Page / setting | Value |
   |---|---|
   | Ecrã — screen size | **TELEMÓVEL / PEQUENO (4–6")** |
   | Ecrã — orientation | **HORIZONTAL** (Landscape) |
   | Android Auto DPI | **200** |
   | Aspeto — app theme | **Escuro** (Dark) |
   | Aspeto — night mode | **Automático** |
   | Áudio — audio stream | **Music / Media** |
   | Áudio — separate streams | **Off** |
   | Automatização — receiver auto-connect/start/reopen toggles | **Off** for this qualification |
   | Localização, if shown | **Phone GPS** |
   | O seu carro | Keep the default name; right-hand drive **Off** |

   Receiver automation is separate from Tesla Hub's verified Bluetooth connection automation; keep Tesla Hub's existing automatic mode enabled.
4. Allow normal notifications and Nearby Devices prompts. Microphone/location can be allowed while using the app; accept the display-over-other-apps prompt for Self Mode if requested. Leave optional system-settings modification off. Tap **Concluir** on the final page. No root, Shizuku, privileged OEM-app controls, or replacement VPN is part of this test.
5. Check the receiver's display/video settings: **720p**, **H.264**, **30 FPS**. These are the fork's initial defaults, but the wizard's hardware recommendations can overwrite resolution/codec; restore the qualification profile after completing it. Keep automatic receiver launch disabled for qualification; Tesla Hub's existing Bluetooth server automation is separate.
6. Return to Tesla Hub's receiver screen and tap **Start phone receiver**.
7. If Android Auto displays first-use permissions/terms for the receiver connection, complete those prompts on the phone. Wait up to **30 seconds**.

**Expected:** A real Android Auto dashboard appears **on the OPPO**. Open Waze or Google Maps from Android Auto's application list. Maps should draw and respond to ordinary touch. Tesla Hub's diagnostic query must show transport started and a growing rendered-frame count, with actual decoded dimensions. Requested/advertised dimensions alone do not prove video.

**If it fails:** Return to Tesla Hub and use **Copy receiver diagnostics**. Send the visible receiver error and whether the Android Auto server notification is still active. Leave USB connected so the receiver's crash buffer and relevant protocol/video log lines can be inspected. Do not send full account/session logs publicly.

## 4. Stop and reconnect — OPPO only

1. Return to Tesla Hub and tap **Stop phone receiver**.
2. Wait a few seconds and tap **Refresh receiver diagnostics**. It should show disconnected/not projecting; video evidence from the previous session must not remain active.
3. Verify Tesla Hub's ordinary server and local route remain under their existing manual/automatic control. Stopping this separate receiver must not replace or tear down the working local VPN.
4. With Android Auto's headunit server still running, tap **Start phone receiver** again. Confirm real video and touch return.

**Expected:** Two real sessions work, with a truthful disconnected state between them. No fabricated HTML Android Auto UI, phone-screen capture, root, or paid receiver is involved.

**If it fails:** Copy receiver diagnostics after the failed transition and report whether the first or second session failed. If the local route stopped or its VPN changed, report that immediately and preserve both apps' state for inspection.

## 5. Local connection and Bluetooth audio — parked Tesla

Perform this only after the OPPO-only receiver/video test works.

1. Keep the ordinary Tesla phone/audio Bluetooth pairing connected. Enable Personal Hotspot manually and connect Tesla Wi-Fi to it.
2. Let Tesla Hub's previously verified automation start its local services. Open **http://169.254.77.1:8765/** in the parked Tesla and open **Diagnostics**.
3. Start the phone receiver on the OPPO. Confirm the Tesla diagnostics show its live receiver state and decoded-video metadata. **The Tesla screen still shows diagnostics, not Android Auto video, in Phase 3.**
4. In real Android Auto on the OPPO, play a short supported audio item or a navigation prompt. Check that the intended sound is heard through Tesla Bluetooth. If it comes from the phone, report the actual route; configured audio sink entries do not prove the route.
5. Verify ordinary Tesla/phone audio and hands-free call handling still work. A short parked test is enough; report any interference, echo, loopback behavior, or call failure.
6. Stop the receiver and reload the local launcher. Check both normal and parked expanded browser modes if convenient. The connection should remain stable.

**Send results when repeating this test:** actual media/navigation audio route, whether hands-free calling works, whether the local launcher stays connected, and any receiver error. The OPPO video, touch, stop/reconnect and local VPN checks passed on the bench; the owner has accepted the fresh parked-car test. Phase 4 browser video and touch can now be developed.

Official setup reference: [Android Auto developer headunit-server instructions](https://developer.android.com/training/cars/testing/dhu). Receiver source, patch, licenses, and build instructions are in [third_party/open-headunit/README.md](third_party/open-headunit/README.md).
