# Tesla Hub

Tesla Hub is a local Android app and browser launcher for an OPPO phone and a Tesla connected to its hotspot. Phase 1 provides a manually started phone server, a touch-friendly Tesla browser page, and diagnostics. Android Auto projection, Bluetooth automation, and hotspot automation are future work.

## Build

Use JDK 17 and Android SDK platform 36 with Build Tools 35.0.0. Set `ANDROID_SDK_ROOT` or install the SDK through Android Studio, then run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

On macOS/Linux, run `./gradlew testDebugUnitTest assembleDebug`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. No signing secrets are needed. GitHub Actions uploads the same APK as `TeslaHub-debug` after a successful build.

## First use

1. Install the debug APK on the OPPO Find X3 Pro.
2. Enable the phone's normal internet-sharing Wi-Fi hotspot. Tesla Hub does not toggle it in Phase 1.
3. Open Tesla Hub and tap **Start local server**. Keep the server notification active.
4. Connect the Tesla to the OPPO hotspot. In the Tesla browser, enter the **Fallback address** displayed by the app, including `:8765/`.
5. Open **Diagnostics** in the Tesla launcher to record viewport and browser capabilities.

The `teslahub.local` address is a preferred experiment, not a guaranteed hostname. The app advertises an mDNS service but Android may assign a different host name and Tesla may not resolve mDNS. Use the numeric URL first.

Stremio Web, RTP Play, and Vodafone TV open directly in the Tesla browser. Their default URLs are configurable under **Settings** and stored only in that browser. Fullscreen Theater behavior and provider playback are not claimed. Tesla's native restrictions remain in effect.

See [TESTING.md](TESTING.md) for exact device steps, [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for failure cases, and [ROADMAP.md](ROADMAP.md) for the next phases.
