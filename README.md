# Tesla Hub

Tesla Hub is a local Android app and browser launcher for an OPPO phone and a Tesla connected to its hotspot. Phase 1 provides a manually started phone server, a touch-friendly Tesla browser page, and diagnostics. Android Auto projection, Bluetooth automation, and hotspot automation are future work.

## Build

Use JDK 17 and Android SDK platform 36 with Build Tools 35.0.0. Set `ANDROID_SDK_ROOT` or install the SDK through Android Studio, then run:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

On macOS/Linux, run `./gradlew clean testDebugUnitTest lintDebug assembleDebug`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Kotlin incremental compilation is disabled for this small module because a stale constructor call caused the physical version 0.1.4 crash. No signing secrets are needed. GitHub Actions uploads the same APK as `TeslaHub-debug` after a successful build.

## First use

1. Install the debug APK on the OPPO Find X3 Pro.
2. Enable the phone's normal internet-sharing Wi-Fi hotspot. Tesla Hub does not toggle it in Phase 1.
3. Open Tesla Hub and tap **Start local server**. Keep the server notification active.
4. Tap **Start local route test (experimental)** → **Continue**, and approve Android's VPN request if shown. Wait for the route phone check to pass. Keep its notification active too.
5. Connect the Tesla to the OPPO hotspot. In the Tesla browser, enter **`http://169.254.77.1:8765/`** and save it as a favorite. This is the tested car address; **Copy local route test URL** copies it from the app.
6. Open **Diagnostics** in the Tesla launcher to record viewport and browser capabilities.

Version 0.1.1 checks the HTTP response on the phone, recovers failed listeners, and offers **Open launcher on phone** and **Copy connection diagnostics**. If the phone loads the launcher but the Tesla refuses the connection, follow Test 3 in [TESTING.md](TESTING.md). A passed phone check does not confirm access from the car.

Version 0.1.6 includes the version 0.1.4 startup crash fix, stabilizes status-screen scrolling, and uses the **experimental local route test** at the link-local address `http://169.254.77.1:8765/`. Android's VPN ingress filtering blocked the previous non-link-local address; the observed TUN packets were IPv6 controls, not HTTP requests. The route uses Android's VPN API with explicit consent and no remote VPN server. The launcher remains on the OPPO, as required by the owner. Trusted HTTPS is not configured; the tested launcher connection works over local HTTP.

**Physical result on 12 October 2026:** Version 0.1.6 stayed open on the OPPO and served the launcher, assets, health check, and diagnostics to the PC on its hotspot. The owner then confirmed the launcher works in the **Tesla Model 3 browser** at `169.254.77.1:8765`; the screenshot shows all four principal buttons and **Local browser connected**. The previously reported Tesla software version is **2026.32.7**. This verifies the local launcher connection on this setup. Provider playback, Android Auto, browser capability values, and long-duration reliability still need their own tests.

The `teslahub.local` address remains an experiment. The app advertises an mDNS service but Android may assign a different host name and Tesla may not resolve mDNS. The ordinary RFC 1918 **Fallback address** worked on the OPPO and PC but was refused by this Tesla. Use the verified link-local URL above for the car. Details of the failed routes and successful fix are in [ARCHITECTURE.md](ARCHITECTURE.md).

Stremio Web, RTP Play, and Vodafone TV open directly in the Tesla browser. Their default URLs are configurable under **Settings** and stored only in that browser. Fullscreen Theater behavior and provider playback are not claimed. Tesla's native restrictions remain in effect.

See [TESTING.md](TESTING.md) for exact device steps, [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for failure cases, and [ROADMAP.md](ROADMAP.md) for the next phases.
