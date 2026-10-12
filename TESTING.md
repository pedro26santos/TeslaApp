# Physical testing: Phase 1

Only test browser functions while the Tesla is parked. The build verifies compilation, not OPPO or Tesla behavior.

## Test 1 — OPPO Find X3 Pro

**On OPPO**

1. Install `app/build/outputs/apk/debug/app-debug.apk`. If installing by USB, enable developer options and USB debugging, then run `adb install -r app/build/outputs/apk/debug/app-debug.apk` from the repository. You can also transfer the APK to the phone and open it there.
2. Turn on the normal **Personal Hotspot / Wi-Fi hotspot with internet sharing** in ColorOS. Note the phone's Android and ColorOS versions from Settings → About device.
3. Open **Tesla Hub**, grant notification permission if prompted, and tap **Start local server**.
4. Wait five seconds. Read **Local Server**, **Fallback address**, **All local addresses**, and **mDNS**. Tap **Copy fallback URL** and paste it into a note if useful. Leave the server running.

**Expected result:** The app says Local Server: Running and shows at least one `http://192.168...:8765/`, `http://172.16-31...:8765/`, or `http://10...:8765/` URL. The notification remains active. A numeric URL is sufficient even if mDNS fails.

**If it fails, send:** exact status text, Android/ColorOS version, hotspot state, and whether the notification appeared. Do not send your hotspot password.

## Test 2 — Tesla Model 3 browser

**On OPPO**

1. Keep hotspot and Tesla Hub server running. Keep the fallback URL visible or copied.
2. Watch whether **Tesla Browser** changes to Connected after the Tesla page opens.

**On Tesla**

1. While parked, connect Wi-Fi to the OPPO hotspot and confirm it is connected.
2. Open the Tesla browser and enter the **exact numeric fallback URL**, including `http://` and port `8765`. Save it as a favorite if it works.
3. Confirm all four principal buttons—Android Auto, Stremio, RTP Play, Vodafone TV—are visible. Tap **Diagnostics** and note the viewport, DPR, user agent, secure context, WebCodecs, WebRTC, MSE, EME, H.264, and AAC values. Use **Download report** if the browser supports it.
4. Tap **Android Auto** and confirm it says the module is not installed yet. Return to the launcher. Open Stremio and RTP Play separately to check whether their sites load. Use the browser back control after each. Open Vodafone TV from its diagnostics panel and record the exact message. Do not enter account credentials as part of this test unless you decide to test provider access separately.
5. Optionally try `http://teslahub.local:8765/` and record whether it resolves. The numeric URL remains the working fallback.

**Expected result:** The launcher loads from the phone, all four buttons remain visible, diagnostics show real browser values, and the phone reports Tesla Browser: Connected while the page stays open. Provider sites may reject Tesla or normal browser playback; this is an observation, not a Phase 1 build failure.

**If it fails, send:** the exact Tesla browser URL and error, the Tesla Wi-Fi IP/gateway if visible, the OPPO's fallback URL and all-address list, whether the server notification remained active, and any diagnostic report. For Vodafone, include the exact WF052 or other text. Avoid screenshots containing personal account details, passwords, or hotspot credentials.
