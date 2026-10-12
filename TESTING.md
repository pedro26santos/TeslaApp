# Physical testing: Phase 1

Only test browser functions while the Tesla is parked. The build verifies compilation, not OPPO or Tesla behavior.

## Test 1 — OPPO Find X3 Pro

**On OPPO**

1. Install `app/build/outputs/apk/debug/app-debug.apk`. For USB installation, enable developer options and USB debugging, connect the OPPO by USB, accept the phone's debugging prompt, then run this command in PowerShell from this workspace: `& '.\.tooling\android-sdk\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'`. The local `.tooling` folder contains the SDK installed for this build and is not committed to Git. You can also transfer the APK to the phone and open it there.
2. Turn on the normal **Personal Hotspot / Wi-Fi hotspot with internet sharing** in ColorOS. Note the phone's Android and ColorOS versions from Settings → About device.
3. Open **Tesla Hub**, grant notification permission if prompted, and tap **Start local server**. Version 0.1.1 and later include connection checks; confirm the version at the top of the app after updating.
4. Wait five seconds. Read **Local Server**, **Phone server check**, **Fallback address**, **All local addresses**, and **mDNS**. Tap **Copy fallback URL** and paste it into a note if useful. Leave the server running.
5. Tap **Open launcher on phone**. Confirm the phone browser opens the page with all four main buttons. Return to Tesla Hub. Opening the page on the phone must not make **Tesla / external browser** say Connected.

**Expected result:** The app says Local Server: Running (phone check passed), and the phone browser loads the launcher. The app shows at least one `http://192.168...:8765/`, `http://172.16-31...:8765/`, or `http://10...:8765/` URL. The notification remains active. A numeric URL is sufficient even if mDNS fails. The phone check verifies the local HTTP response; Tesla reachability still needs Test 2.

**If it fails, send:** tap **Copy connection diagnostics** and paste the text into this chat, plus the phone browser's exact error and Android/ColorOS version. Do not send your hotspot password.

## Test 2 — Tesla Model 3 browser

**On OPPO**

1. Keep hotspot, Tesla Hub server, and the local route running. For version 0.1.6, follow Test 5 and keep `http://169.254.77.1:8765/` visible or copied with **Copy local route test URL**.
2. Watch whether **Tesla / external browser** changes to Connected after the Tesla page opens. This indicator tracks a browser on another device; a laptop or second phone can also trigger it.

**On Tesla**

1. While parked, connect Wi-Fi to the OPPO hotspot and confirm it is connected.
2. Open the Tesla browser and enter **`http://169.254.77.1:8765/`**, including `http://` and port `8765`. This address passed the owner's car test on 12 October 2026. Save it as a favorite.
3. Confirm all four principal buttons—Android Auto, Stremio, RTP Play, Vodafone TV—are visible. Tap **Diagnostics** and note the viewport, DPR, user agent, secure context, WebCodecs, WebRTC, MSE, EME, H.264, and AAC values. Use **Download report** if the browser supports it.
4. Tap **Android Auto** and confirm it says the module is not installed yet. Return to the launcher. Open Stremio and RTP Play separately to check whether their sites load. Use the browser back control after each. Open Vodafone TV from its diagnostics panel and record the exact message. Do not enter account credentials as part of this test unless you decide to test provider access separately.
5. Optionally try `http://teslahub.local:8765/` and record whether it resolves. The numeric URL remains the working fallback.

**Expected result:** The launcher loads from the phone, all four buttons remain visible, diagnostics show real browser values, and the phone reports Tesla / external browser: Connected while the page stays open. **Connections from other devices** increases and shows the client address. Provider sites may reject Tesla or normal browser playback; record the result separately from launcher reachability.

**If it fails, send:** the exact Tesla browser URL and error, the Tesla Wi-Fi IP/gateway if visible, the OPPO's fallback URL and all-address list, whether the server notification remained active, and any diagnostic report. For Vodafone, include the exact WF052 or other text. Avoid screenshots containing personal account details, passwords, or hotspot credentials.

## Test 3 — phone launcher works, Tesla reports ERR_CONNECTION_REFUSED

This is the historical comparison for the ordinary private fallback address. Version 0.1.6's link-local URL passed the car connection test; use Test 5 for the current setup.

**On OPPO**

1. Turn off the phone's ordinary Wi-Fi connection, leave mobile data on, and enable Personal Hotspot. If switching Wi-Fi off also stops the hotspot, turn Personal Hotspot back on. This test uses cellular internet sharing and avoids selecting an address on a separate upstream Wi-Fi network.
2. In Tesla Hub, tap **Stop local server**, then **Start local server**. Wait five seconds. Copy the newly displayed fallback URL; the address can change when the hotspot restarts.
3. Open that new URL in the phone browser and confirm the launcher still opens. Return to Tesla Hub and note **Connections from other devices** before testing the car.

**On Tesla**

1. While parked, disconnect and reconnect Wi-Fi to the exact OPPO Personal Hotspot name. Confirm connected status.
2. Enter the copied URL with an explicit `http://` prefix and `:8765/` suffix. Use the newly copied value rather than an old bookmark.
3. If it fails, note the exact error and check whether **Connections from other devices** increased on the phone. Try each URL under **All local addresses** if more than one is present.

**Expected result:** The launcher loads. A connection refusal before the HTTP request is received cannot be caused by the launcher's HTML. A passed phone check alone does not prove that other devices can reach the phone.

**If it fails:** Copy the connection diagnostics after the Tesla attempt and send them with the exact Tesla error and Tesla software version. If a laptop or second phone is available, connect it to the same OPPO hotspot and open the same URL in its browser. Report whether that device loads the launcher. This separates phone/hotspot reachability from behavior specific to the Tesla. Keep safety restrictions, native playback restrictions, DRM/HDCP, and Android permission checks in effect.

## Test 4 — version 0.1.6 link-local address, OPPO and PC

The existing address works on the owner's PC, while Tesla software 2026.32.7 refuses it. Versions 0.1.2 and 0.1.3 timed out at the non-link-local VPN address; their TUN counters were affected by IPv6 interface controls, not the PC's HTTP traffic. Version 0.1.6 includes the 0.1.4 startup crash fix, stabilizes status scrolling, and uses a link-local address, which Android excludes from its VPN-address ingress filtering. The owner requires strictly local hosting. This test checks Android/ColorOS delivery through the second, phone-owned address using normal Android VPN consent. It does not yet configure trusted HTTPS or prove Tesla compatibility. Complete this PC test before going to the car again.

**Install on OPPO**

1. Connect the unlocked OPPO to the PC by USB and accept USB debugging if prompted.
2. From this workspace in PowerShell run: `& '.\.tooling\android-sdk\platform-tools\adb.exe' install -r '.\app\build\outputs\apk\debug\app-debug.apk'`.
3. Confirm `Success`, open Tesla Hub, and confirm **App version: 0.1.6**. Allow notifications if prompted. An uninstall is not required for a normal update. If ADB shows `unauthorized`, unlock the OPPO and accept its USB debugging authorization dialog; then repeat the installation command. Confirm the app stays open before starting network checks.

**On OPPO**

1. Keep mobile data on and enable Personal Hotspot. Start the usual local server.
2. Tap **Start local route test (experimental)**. Read the explanation, tap **Continue**, then approve Android's VPN connection request. This can replace another active VPN; the test has no remote VPN server and no default route.
3. Wait ten seconds. Scroll to **Experimental local route** and check its status. It should say **Phone check passed; link-local address; PC test required; HTTPS not configured**. If it reports a failure, copy diagnostics immediately.
4. Tap **Copy local route test URL**. The new URL is `http://169.254.77.1:8765/`. Do not reuse the previous `100.127.255.1` address.

**On PC**

1. Connect Wi-Fi to the same OPPO hotspot.
2. Open the copied local route test URL in a fresh browser tab. Keep the original numeric URL available as a control.
3. Confirm the Tesla Hub launcher opens through the new URL. Reload it twice and open **Diagnostics** to check that repeated connections and the API work. Also open `http://169.254.77.1:8765/api/health`; it must show **Tesla Hub OK**.
4. On the OPPO, note **Connections from other devices**, **Last route packet**, and **Route monitor error**. Tap **Copy connection diagnostics** and paste the report into this chat. Report whether both the original address and the new address work.
5. After recording the result, tap **Stop local route test** on the OPPO. Confirm the test URL disappears and normal internet access still works on the phone and PC. The normal local server can remain running.

**Recorded result, 12 October 2026:** Version 0.1.6 was installed on the OPPO and passed the PC check. The launcher, health endpoint, diagnostics API, CSS, and JavaScript returned HTTP 200 through `169.254.77.1:8765`. The ordinary fallback URL also worked. Home and reopening left the same app process and both foreground services running, with no new crash or route monitor error. The owner subsequently confirmed the Tesla launcher connection in Test 5.

**Expected result:** The phone self-check passes, the PC loads the launcher and the health endpoint through the new address, and stopping the route removes the test address. HTTP is delivered by Android's TCP stack. TUN counters may increase due to IPv6 interface controls; they are not a success/failure criterion for this test. There should be no fatal route monitor error. The normal numeric URL remains the control for diagnosing an OPPO routing failure.

**If it fails, send:** the copied connection diagnostics, whether the Android VPN prompt was accepted, exact phone check text, whether the original URL still works, and the PC browser's exact error. If the TUN counter increases, include the count. Do not send passwords, private keys, certificates containing private keys, or VPN-account credentials.

## Test 5 — parked Tesla at the new link-local URL

**Recorded result, 12 October 2026: PASSED for launcher connectivity.** The owner reported “Working!” and supplied a Tesla browser screenshot showing `169.254.77.1:8765`, the launcher, all four principal buttons, and **Local browser connected**. This confirms page rendering and a working browser/server connection on the car. A downloaded browser report, provider playback, and long-duration/reconnect tests have not yet been supplied. The screenshot remains an ignored local attachment and is not included in Git.

**On OPPO**

1. Confirm **App version: 0.1.6**, Personal Hotspot on, **Start local server** running, and **Start local route test (experimental)** active. Approve the VPN request if needed. Wait for the local-route phone check to pass and the URL to appear.
2. Keep mobile data enabled when taking the phone to the car. Keep both Tesla Hub foreground notifications active. USB is not needed in the car. If the hotspot stops during the move, turn it back on and wait for the phone checks; copy the current ordinary fallback URL for diagnostics.

**On Tesla**

1. Park the vehicle and connect its Wi-Fi to the OPPO's Personal Hotspot.
2. Open the browser and type **`http://169.254.77.1:8765/`**, including `http://`, port `8765`, and the final slash. Use this new address rather than an old bookmark.
3. If the launcher opens, tap **Diagnostics**, verify the phone shows **Tesla / external browser: Connected**, and download the report if available. Report that the new URL works on the car. Android Auto is still a placeholder at this phase.
4. If it fails, photograph the address bar and exact error. Then on the OPPO tap **Copy connection diagnostics** and paste the text into this chat after the car attempt. Keep the app installed; another uninstall does not diagnose a browser refusal.

**Expected result:** The launcher opens through the local phone server and the browser reports connected. This result is now confirmed on the owner's setup with previously reported Tesla software 2026.32.7. Browser capability values and individual provider playback still require the steps in Test 2. Trusted local HTTPS is not configured in 0.1.6 and was not required for this successful launcher connection. The owner's condition for committing the connection fix is satisfied.
