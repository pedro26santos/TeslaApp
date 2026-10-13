# Physical testing: Phase 2 / APK 0.2.1

The 0.1.6 local launcher passed the owner's Tesla test at `http://169.254.77.1:8765/`. Version 0.2.1 adds Bluetooth connection automation. On 13 October 2026, the owner confirmed automatic startup on Tesla Bluetooth connection and shutdown after disconnect. The remaining individual reliability checks below are still useful and are not claimed as passed. Test in a parked car. Keep this file available on the phone before going to the Tesla.

Version 0.2.1 also corrects shutdown of Android's bound local VPN service. Stop commands explicitly close the tunnel and remove its foreground notification; an interrupted old worker cannot shut down a replacement session. Version 0.2.0 exposed this issue during the USB checks on the OPPO and should be updated before testing automatic mode.

## Developer checks completed on 13 October 2026

- Clean APK build, all 18 unit tests passed, and no lint errors.
- APK 0.2.1 (version code 9) installed by USB on the OPPO CPH2173 running Android 14.
- App opened and survived Home/reopen without a crash in its crash buffer.
- Manual startup passed the phone's HTTP checks. The PC on the OPPO hotspot received `Tesla Hub OK` from `http://169.254.77.1:8765/api/health`.
- **Stop Tesla Hub** removed both network services and the `tun0` interface. Restart passed the phone checks again and restored both foreground services.
- At the end of these developer checks, automatic mode was left off with no car selected for the owner's setup and car test.

## Owner's car result reported on 13 October 2026

- The owner reports the server starts automatically when the Tesla connects through Bluetooth and stops when Bluetooth disconnects.
- Photos show APK 0.2.1, the selected Tesla connected, an active connection monitor, a 60-second disconnect setting, no automation error, and the local launcher connected in the Tesla browser.
- Normal and expanded browser diagnostics are transcribed in [TESLA_BROWSER_REPORT.md](TESLA_BROWSER_REPORT.md).
- Exact shutdown timing, reconnect cancellation, locked-screen timing, changed-delay/persistence checks, reboot recovery, manual ownership, and audio/call regression were not separately reported. Keep those checks pending; the successful core milestone may be committed without describing them as verified.

## 1. Install and check the manual connection first

**On the PC / OPPO**

1. Connect the unlocked OPPO by USB, enable USB debugging, and accept the debugging prompt if shown.
2. From the repository in PowerShell run `& '.\.tooling\android-sdk\platform-tools\adb.exe' -s 445dcb67 install -r '.\app\build\outputs\apk\debug\app-debug.apk'`. Confirm `Success`. A normal update does not require uninstalling; uninstalling would delete the saved settings.
3. Open Tesla Hub and confirm **App version: 0.2.1**. The app must stay open. Automatic connection is off by default.
4. Enable the OPPO's internet-sharing **Personal Hotspot**, keep mobile data on, and tap **Start Tesla Hub**. Approve the local VPN request if shown. This starts both parts of the known local connection.
5. Connect the PC or parked Tesla to the hotspot and open **`http://169.254.77.1:8765/`**. Confirm the launcher loads, then open **Diagnostics**.

**Expected:** Manual launcher access works as it did in 0.1.6. The main screen says **Server control: Manual**. Declining Bluetooth permissions must not break this manual path.

**If it fails:** Keep USB connected if possible and send the exact error plus **Copy connection diagnostics**. If the app closes, report that before testing Bluetooth; its crash buffer is needed. Do not uninstall as a troubleshooting step.

## 2. Set up automatic mode

**On OPPO, while parked**

1. Tap **Stop Tesla Hub** to begin with no manually started services. This also disables any previously enabled automatic monitor.
2. Open **Connection settings** → **Select paired Tesla**. Allow **Nearby Devices** when Android asks. Choose the Tesla's existing **phone/audio Bluetooth pairing**, not a Bluetooth LE phone-key device. Pair it in Android Bluetooth settings first if it is absent.
3. Leave **Disconnect delay: 60 seconds** and **Stop automatic services after disconnect** enabled for the first test.
4. Turn **Automatic Tesla connection** on. Allow notifications and Android's local VPN consent if prompted. The connection monitor notification must appear. If notifications are disabled in system settings, enable them and use **Resume automatic monitoring**.
5. If the Tesla is already connected by Bluetooth, the app should detect it through the phone/audio profile query and start its services. If it is disconnected, it should show **Waiting for selected Tesla** and leave networking stopped.

**Expected:** The selected car and settings are saved; the monitor stays running. No discovery scan, root, Shizuku, or account credentials are required. Enabling the mode does not change Bluetooth audio routing or initiate a new phone/car pairing.

**If it fails:** Send the status text and **Copy connection diagnostics**. The exported report deliberately omits paired-device names and MAC addresses. Describe whether the Tesla's normal phone/audio Bluetooth connection is active in the phone/car settings.

## 3. Automatic startup in the Tesla

**On OPPO**

1. Leave automatic mode on and tap the phone's Home button. Keep its connection monitor notification visible. Do not press Android **Force stop**; force-stopped apps do not receive normal background broadcasts until reopened.
2. Make the normal Bluetooth connection to the selected Tesla. If a test disconnect is needed, use the phone's Bluetooth device menu for the selected Tesla and reconnect it there.
3. Wait up to ten seconds, then open Tesla Hub to inspect the state if necessary. It should say **Selected Tesla connected**, **Local Server: Running**, **Server control: Automatic**, and show the Tesla URL.
4. Enable **Personal Hotspot** if it is not already on. The connection notification's **Hotspot settings** action opens the phone settings. A detected hotspot interface is only a network hint; confirm internet sharing in ColorOS.

**On Tesla**

1. Keep the car parked, connect Wi-Fi to the OPPO hotspot, and open **`http://169.254.77.1:8765/`**.
2. Confirm the launcher reports **Local browser connected**. Open **Diagnostics** → **Download report** if available and send the report after the test.

**Expected:** Bluetooth starts the server and the existing local address automatically. Hotspot activation is user assisted on the Android 14 OPPO. The same launcher URL works. Android Auto remains a Phase 3 placeholder.

**If it fails:** Copy diagnostics after the attempt. Include whether the monitor notification remained present, whether Bluetooth connects normally to the car, whether opening Tesla Hub alone resumes the operation, and the browser error. An Android/ColorOS background-start refusal should appear as an automation error or **Resume Tesla Hub** notification; it must not be mistaken for successful startup.

## 4. Reconnect during the grace period

**On OPPO / Tesla, while parked**

1. Begin with automatic mode connected and **Server control: Automatic**. Keep the hotspot active.
2. Disconnect only the selected Tesla in the phone's Bluetooth device menu. Note the time. Check the app's **Disconnect grace period** and remaining seconds.
3. Reconnect within **30 seconds**. The countdown should disappear. Do not tap manual startup buttons during this test, because that gives the services manual control.
4. Wait until more than 60 seconds have passed since the original disconnect and reload the Tesla launcher.

**Expected:** No shutdown after reconnecting during the configured grace period; the launcher still loads. A phone/audio profile closing while another selected-car profile remains connected should not falsely shut down the session. Connecting or disconnecting a different paired device must not control Tesla Hub.

**If it fails:** Send diagnostics immediately after the wrong transition, the actual disconnect/reconnect times, and whether the phone still showed the Tesla connected. Exact device names or addresses are not needed.

## 5. Shutdown after a sustained disconnect

1. Start from automatic mode connected and both automatic services running.
2. Disconnect the selected Tesla and leave it disconnected for at least **70 seconds** with the default 60-second delay.
3. Check that the ordinary local server and Tesla local connection stop and their notifications disappear. The connection monitor stays active, waiting for the selected car. The manually enabled Personal Hotspot must remain on.
4. Reconnect the Tesla and confirm automatic startup and the local launcher work again.
5. Repeat once with the phone screen locked. Record actual timing if ColorOS delays shutdown or stops the monitor. The timer uses elapsed time, but service execution still depends on Android scheduling; do not report exact screen-off timing as proven without this check.

**Expected:** Shutdown occurs after the configured delay for services started by automatic mode; reconnecting starts them again. The app does not turn off a hotspot it did not enable.

**If it fails:** Report whether the monitor survived, actual elapsed time, which service remained running, and the copied diagnostics. If **Stop automatic services after disconnect** was off or **Server control: Manual** was shown, remaining services are intentional; repeat with the test's required settings.

## 6. Saved settings, manual control, and recovery

1. In **Connection settings**, set the disconnect delay to **10 seconds**, go back to Tesla Hub, close/reopen the settings screen, and confirm the Tesla selection, delay, and switches persist. Restore **60 seconds** after testing. Invalid values below 10 or above 600 must not save.
2. Test the 10-second disconnect once, waiting at least 15 seconds, and confirm the configured delay is used.
3. Turn **Stop automatic services after disconnect** off, reconnect/disconnect, and confirm networking stays running after the delay. Restore it to on.
4. Tap **Start Tesla Hub** to take manual control, disconnect the Tesla, and confirm manual services remain running. Use **Stop Tesla Hub** to stop them and disable automatic mode.
5. With automatic mode enabled, reboot the OPPO and unlock it. Check the monitor notification. On Android/ColorOS builds that block its background restart, open Tesla Hub or tap **Resume Tesla Hub** and check the reported error. Reboot/background delivery is unverified until this physical test passes.
6. On the parked car, check a short media playback session through the normal phone-to-Tesla Bluetooth route. Existing call handling and audio should remain unchanged; report any observed regression. Tesla Hub only observes those connections.

**Send after these tests:** the downloaded Tesla browser report if available, the OPPO connection diagnostics, and which numbered tests passed or failed. The owner's 13 October report verifies the core startup/shutdown milestone; further reliability claims require the corresponding results above.

## Optional MacroDroid hotspot fallback

Tesla Hub works with manual Personal Hotspot activation. MacroDroid is optional and is not bundled or required. Its [Bluetooth Event documentation](https://macrodroidforum.com/wiki/index.php?title=Trigger%3A_Bluetooth_Event) supports selecting a paired device; its [HotSpot action documentation](https://macrodroidforum.com/wiki/index.php/Action%3A_HotSpot_On/Off) says device support varies and uses private APIs. This is not a promise that it works on the OPPO.

1. If you already use MacroDroid, create a macro with **Bluetooth Event → Device Connected**, selecting the same Tesla phone/audio pairing.
2. Add **HotSpot On/Off → Enable HotSpot** using the app's supported setup. Do not add an unconditional hotspot-off macro.
3. Test the macro while parked. Confirm the hotspot is actually on in ColorOS, the car has internet through it, and the local launcher still opens.
4. If hotspot activation fails or asks for an unsupported setup, disable that macro and use Tesla Hub's **Hotspot settings** notification action. Send the exact MacroDroid error before considering a privileged backend. No root or security-setting changes are part of Tesla Hub's current implementation.
