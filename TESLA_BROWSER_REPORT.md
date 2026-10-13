# Tesla browser observations — 13 October 2026

## Evidence and scope

The owner confirmed that Tesla Hub **0.2.1** starts its server automatically when the selected Tesla connects through Bluetooth and stops it when Bluetooth disconnects. Four diagnostic photos show the local page running in the Tesla browser at `http://169.254.77.1:8765/`.

These are transcribed observations from the owner's photos, not a downloaded JSON report or a benchmark. The first three photos show the normal browser; the fourth shows the expanded browser that the owner uses while parked. Photos and personal network addresses are not included in Git.

Device: OPPO Find X3 Pro / CPH2173, Android 14. Car: owner's Tesla Model 3; software **2026.32.7** was reported earlier and was not remeasured by these photos.

## Viewports

All dimensions below are browser-reported CSS pixels.

| Browser mode | `innerWidth × innerHeight` | `screen.width × screen.height` | Device pixel ratio | Orientation |
| --- | --- | --- | --- | --- |
| Normal | **773 × 601** | 1254 × 784 | 1.5299999713897705 (approximately 1.53) | landscape-primary |
| Expanded, while parked | **1256 × 706** | 1254 × 784 | 1.5299999713897705 (approximately 1.53) | landscape-primary |

The normal viewport is visible in photo `1000076787.jpg`; the expanded viewport is visible in `1000076796.jpg`. Do not replace either reported width with the `screen.width` value; the slight difference is an observed browser result. These values describe available page space, not a negotiated Android Auto video resolution or a physical display measurement.

User agent shown in both modes:

```text
Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36
```

Platform: `Linux x86_64`. Browser connected: **Yes**. Secure context: **No** in both photos.

## Capabilities visible in the normal-browser photos

| Probe | Reported result |
| --- | --- |
| WebSocket | Yes |
| WebRTC (`RTCPeerConnection` presence) | Yes |
| WebCodecs (`VideoDecoder` presence) | No |
| H.264 WebCodecs configuration | Unavailable |
| MediaSource / MSE | Yes |
| Encrypted Media Extensions (EME) | No |
| Widevine API | Unavailable |
| ClearKey API | Unavailable |
| H.264 video (`canPlayType`) | probably |
| H.265 video (`canPlayType`) | probably |
| AAC audio (`canPlayType`) | probably |
| Cookies enabled | Yes |
| localStorage | Yes |
| Service workers | No |

The expanded-browser photo does not show its media feature panel. Its viewport and connection values are recorded above; identical media capabilities across modes are not claimed.

## Connection observations

The normal-browser photos show the local server running, passing its phone HTTP check, and serving an external client. The selected Tesla is connected, the connection monitor is running, and the configured disconnect delay is **60 seconds**. Automation error, request error, and route monitor error are **None**. No countdown is active in the connected state. The local route reports a passing phone check, and Android reports an active VPN.

The owner additionally reports successful automatic shutdown after disconnecting. Exact elapsed shutdown time, reconnect cancellation, locked-screen execution, reboot recovery, changed-delay settings, and audio/call regression were not individually reported. Preserve those reliability checks in [TESTING_PHASE2.md](TESTING_PHASE2.md).

Hotspot control remains manual in 0.2.1. The diagnostic text explicitly says a hotspot interface was detected and control remains manual.

## Consequences for subsequent implementation

- The existing page already reports resize and orientation changes. Android Auto layout must follow the live stream container size in either browser mode; these measurements are reference cases rather than fixed layout sizes.
- Preserve the negotiated video aspect ratio. Scale into the actual content rectangle, account for letterboxing, and map touch coordinates through that rectangle. Device pixel ratio must not be applied twice to CSS input coordinates. Expanded mode is a browser state observed while parked; no vehicle-motion detector or automatic expansion has been implemented.
- WebRTC and MSE remain transport candidates. API presence and `canPlayType: probably` do not demonstrate a decoded stream, codec negotiation, usable frame rate, latency, or thermal performance. Prove a real Android Auto receiver session before building browser projection.
- WebCodecs and EME results apply to this local HTTP origin. The [W3C WebCodecs specification](https://www.w3.org/TR/webcodecs/#videodecoder-interface) exposes `VideoDecoder` in secure contexts, and the [W3C EME specification](https://www.w3.org/TR/encrypted-media/#navigator-extension) similarly restricts `requestMediaKeySystemAccess`. The photos therefore do not establish whether either API works at a trusted HTTPS origin. No transport may assume WebCodecs is available on the current page.
- Local EME results do not determine Vodafone TV playback at its HTTPS origin. Provider navigation, account access, DRM acceptance, and playback remain separate physical tests.

No new APK or runtime behavior is introduced by recording these observations.

## Phase 3 receiver coexistence — 13 October 2026

The owner subsequently reported the fresh parked-car Phase 3 checklist working as expected. Two Tesla-browser photos show a fresh receiver query, video decoded on the phone at **1280 × 720 H.264/AVC**, announced **200 DPI / 30 FPS**, and rendered-frame counts **2,588** then **2,859**. Last-frame ages are **6 ms** then **23 ms**; receiver error is **None**. These observations prove a live receiver session alongside access to the local diagnostics page, not browser video playback or measured FPS.

The photos identify receiver **3.5.0-beta4-teslahub1** and Android Auto **17.7.663654-release**. Stream, encoder, bitrate, browser frame rate and touch mapping are explicitly unavailable in this Phase 3 build. The configured audio sink is **5: PCM**; audible output cannot be inferred from that setting. The owner's overall checklist acceptance is recorded in [TESTING_PHASE3.md](TESTING_PHASE3.md), with individually itemized audio/call/timing observations retained for the next test.
