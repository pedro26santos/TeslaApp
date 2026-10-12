# Physical testing: Phase 1

Only test browser functions while the Tesla is parked. The build verifies compilation, not OPPO or Tesla behavior.

## Test 1 — OPPO Find X3 Pro

**On OPPO**

1. Install `app/build/outputs/apk/debug/app-debug.apk`. For USB installation, enable developer options and USB debugging, connect the OPPO by USB, accept the phone's debugging prompt, then run this command in PowerShell from this workspace: `& '.\.tooling\android-sdk\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'`. The local `.tooling` folder contains the SDK installed for this build and is not committed to Git. You can also transfer the APK to the phone and open it there.
2. Turn on the normal **Personal Hotspot / Wi-Fi hotspot with internet sharing** in ColorOS. Note the phone's Android and ColorOS versions from Settings → About device.
3. Open **Tesla Hub**, grant notification permission if prompted, and tap **Start local server**. Version 0.1.1 adds connection checks; confirm the version at the top of the app after updating.
4. Wait five seconds. Read **Local Server**, **Phone server check**, **Fallback address**, **All local addresses**, and **mDNS**. Tap **Copy fallback URL** and paste it into a note if useful. Leave the server running.
5. Tap **Open launcher on phone**. Confirm the phone browser opens the page with all four main buttons. Return to Tesla Hub. Opening the page on the phone must not make **Tesla / external browser** say Connected.

**Expected result:** The app says Local Server: Running (phone check passed), and the phone browser loads the launcher. The app shows at least one `http://192.168...:8765/`, `http://172.16-31...:8765/`, or `http://10...:8765/` URL. The notification remains active. A numeric URL is sufficient even if mDNS fails. The phone check verifies the local HTTP response; Tesla reachability still needs Test 2.

**If it fails, send:** tap **Copy connection diagnostics** and paste the text into this chat, plus the phone browser's exact error and Android/ColorOS version. Do not send your hotspot password.

## Test 2 — Tesla Model 3 browser

**On OPPO**

1. Keep hotspot and Tesla Hub server running. Keep the fallback URL visible or copied.
2. Watch whether **Tesla / external browser** changes to Connected after the Tesla page opens. This indicator tracks a browser on another device; a laptop or second phone can also trigger it.

**On Tesla**

1. While parked, connect Wi-Fi to the OPPO hotspot and confirm it is connected.
2. Open the Tesla browser and enter the **exact numeric fallback URL**, including `http://` and port `8765`. Save it as a favorite if it works.
3. Confirm all four principal buttons—Android Auto, Stremio, RTP Play, Vodafone TV—are visible. Tap **Diagnostics** and note the viewport, DPR, user agent, secure context, WebCodecs, WebRTC, MSE, EME, H.264, and AAC values. Use **Download report** if the browser supports it.
4. Tap **Android Auto** and confirm it says the module is not installed yet. Return to the launcher. Open Stremio and RTP Play separately to check whether their sites load. Use the browser back control after each. Open Vodafone TV from its diagnostics panel and record the exact message. Do not enter account credentials as part of this test unless you decide to test provider access separately.
5. Optionally try `http://teslahub.local:8765/` and record whether it resolves. The numeric URL remains the working fallback.

**Expected result:** The launcher loads from the phone, all four buttons remain visible, diagnostics show real browser values, and the phone reports Tesla / external browser: Connected while the page stays open. **Connections from other devices** increases and shows the client address. Provider sites may reject Tesla or normal browser playback; record the result separately from launcher reachability.

**If it fails, send:** the exact Tesla browser URL and error, the Tesla Wi-Fi IP/gateway if visible, the OPPO's fallback URL and all-address list, whether the server notification remained active, and any diagnostic report. For Vodafone, include the exact WF052 or other text. Avoid screenshots containing personal account details, passwords, or hotspot credentials.

## Test 3 — phone launcher works, Tesla reports ERR_CONNECTION_REFUSED

**On OPPO**

1. Turn off the phone's ordinary Wi-Fi connection, leave mobile data on, and enable Personal Hotspot. If switching Wi-Fi off also stops the hotspot, turn Personal Hotspot back on. This test uses cellular internet sharing and avoids selecting an address on a separate upstream Wi-Fi network.
2. In Tesla Hub, tap **Stop local server**, then **Start local server**. Wait five seconds. Copy the newly displayed fallback URL; the address can change when the hotspot restarts.
3. Open that new URL in the phone browser and confirm the launcher still opens. Return to Tesla Hub and note **Connections from other devices** before testing the car.

**On Tesla**

1. While parked, disconnect and reconnect Wi-Fi to the exact OPPO Personal Hotspot name. Confirm connected status.
2. Enter the copied URL with an explicit `http://` prefix and `:8765/` suffix. Use the newly copied value rather than an old bookmark.
3. If it fails, note the exact error and check whether **Connections from other devices** increased on the phone. Try each URL under **All local addresses** if more than one is present.

**Expected result:** The launcher loads. A connection refusal before the HTTP request is received cannot be caused by the launcher's HTML. A passed phone check alone does not prove that other devices can reach the phone.

**If it fails:** Copy the connection diagnostics after the Tesla attempt and send them with the exact Tesla error and Tesla software version. If a laptop or second phone is available, connect it to the same OPPO hotspot and open the same URL in its browser. Report whether that device loads the launcher. This separates phone/hotspot reachability from behavior specific to the Tesla. Do not change firewall, VPN, security, or provider settings to bypass a restriction.
