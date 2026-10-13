# Troubleshooting

## Version 0.2.1 automatic connection does not start

First verify the manual **Start Tesla Hub** path still works. Then follow [TESTING_PHASE2.md](TESTING_PHASE2.md). Select the car's paired phone/audio Bluetooth device, allow Nearby Devices and notifications, enable automatic mode, and approve the local VPN setup if requested. A Bluetooth LE phone-key device is not the selected classic phone/audio connection. If no monitor notification appears, open Tesla Hub or use **Resume automatic monitoring** in connection settings. Android/ColorOS may block a background start; capture the displayed automation error instead of assuming startup succeeded. Android **Force stop** prevents normal receiver delivery until the app is opened again.

## Disconnect does not stop networking

Check the configured delay and **Stop automatic services after disconnect**. Duplicate disconnects do not restart the delay; reconnecting cancels it. **Server control: Manual** means a manual start took control, so automatic cleanup preserves it. Use **Stop Tesla Hub** for an explicit stop, then enable automatic mode from an idle setup to test automatic ownership. The connection monitor intentionally remains active after a timed shutdown. Personal Hotspot remains on because this version did not enable it. If screen-off timing is late, record actual elapsed time and monitor presence for the OPPO scheduling test.

## Hotspot does not turn on automatically

On the Android 14 OPPO, this build uses user-assisted activation. Tap **Hotspot settings** on the Tesla Hub connection notification and enable internet-sharing Personal Hotspot in ColorOS. A local-only hotspot would not supply the car's normal internet services. Root and Shizuku are not required for the supported path. Optional MacroDroid setup is documented in TESTING_PHASE2.md; its hotspot action is not verified on this device.

## App closes immediately in version 0.1.4

The physical OPPO crash log identified `NoSuchMethodError` from a stale `LocalHttpServer` constructor call in the APK. Install **version 0.1.6**, which includes the clean-build fix with Kotlin incremental compilation disabled. An uninstall is not required. After updating, open Tesla Hub, start the local server, and confirm it stays open and reports its phone check. If it closes again, keep USB connected so the crash buffer can be read before another rebuild; do not treat it as a hotspot-settings failure. Version 0.1.6 also avoids relayout for unchanged status text, so scrolling to the controls stays stable.

## No fallback address

Enable the OPPO's **internet-sharing** Wi-Fi hotspot and start the server. Wait five seconds for interface discovery. If the app still says “No Wi-Fi/hotspot interface detected,” record the phone's Android and ColorOS versions, the hotspot screen status, and the app status text. The Phase 1 interface filter may need adjustment for ColorOS's interface name. No silent fallback to cellular or public binding is used.

## Tesla cannot open the numeric URL

**Verified setup:** Version 0.1.6 worked in the owner's Tesla browser at **`http://169.254.77.1:8765/`** on 12 October 2026. Keep the OPPO hotspot, ordinary local server, and **Start local route test (experimental)** active; use **Copy local route test URL** for the car. The ordinary RFC 1918 **Fallback address** was refused by this Tesla. The following private-address checks describe that earlier diagnosis.

Confirm the Tesla's Wi-Fi is connected to the OPPO hotspot, copy the exact fallback URL including `http://` and `:8765/`, and keep the Tesla Hub server notification active. Try each address listed under **All local addresses** if multiple are shown. A Tesla browser “site cannot be reached” result may mean hotspot client isolation, a different interface address, or ColorOS stopping the service. Record the Tesla Wi-Fi IP/gateway when visible and the exact browser error.

If the launcher opens in Chrome on the OPPO but the Tesla reports `ERR_CONNECTION_REFUSED`, the phone-side HTTP server is responding. It remains necessary to test incoming access through the hotspot. Follow Test 3 in [TESTING.md](TESTING.md). Restarting the hotspot can change the numeric URL, so copy it again. Version 0.1.1 distinguishes the phone's connected Wi-Fi interface from other local interface candidates and places hotspot candidates first. Interface names alone do not prove the hotspot role.

**Phone server check: Passed** verifies only the phone's HTTP response. **Connections from other devices** counts accepted TCP connections whose source is not the phone's own bound address, including requests rejected by the subnet check. If the count stays zero during the Tesla attempt, the app has not accepted a connection from the car. If it increases, include **Last request error** and **Copy connection diagnostics** so request rejection can be distinguished from a routing failure.

There are [reports of local-address connection failures in a Tesla-owned issue tracker](https://github.com/teslamotors/fleet-telemetry/issues/423), but the report does not establish the cause on this car. Do not assume a universal Tesla browser restriction without comparing another device on the same hotspot. A Tesla software version and that comparison are needed to investigate firmware-specific behavior.

The authors of [Homepage for Tesla](https://github.com/JesseWebDotCom/homepage-for-tesla/blob/main/README.md) and [Tesla CarPlay](https://github.com/marcraft2/tesla-carplay/blob/master/tesla-doc.md) also describe the browser blocking private IP destinations. The owner's version 0.1.1 test shows a passing phone check but a refused Tesla connection and zero accepted external connections during the car attempt. A subsequent PC test loaded the same launcher URL successfully, confirming external access to the phone server. Focus further investigation on the Tesla browser/network path; its exact restriction is not yet proven. Do not keep reinstalling the APK or repeating the same car URL attempt without new evidence.

## Experimental local route cannot be reached

Update the OPPO to **version 0.1.6**. The previous `100.127.255.1` address timed out; the phone reports Android VPN ingress filtering enabled, and Android filters non-link-local VPN addresses arriving through other interfaces. The new build uses the standard link-local exception with `http://169.254.77.1:8765/`. Approve Android's VPN prompt, keep the OPPO hotspot active, and test this new address from the PC on that same hotspot. Compare the ordinary fallback URL, which can change after a hotspot restart.

Include **localRouteStatus**, **localRoutePackets**, **localRouteLastPacket**, **localRouteMonitorError**, and **externalConnectionCount** from **Copy connection diagnostics**. A nonzero TUN count alone does not prove that a client's HTTP packets arrived: IPv6 interface control traffic was observed in the failed 0.1.3 test. Only a loaded launcher or a successful `/api/health` response proves the complete path. A phone check alone still does not prove PC or Tesla reachability. Version 0.1.6's link-local launcher connection has now passed both PC and Tesla tests on the owner's setup. Trusted HTTPS remains unconfigured, and browser API/provider compatibility requires separate checks.

Stop the local route test after collecting the result. The service opts out of always-on VPN and registers only the single `/32` link-local test route for Tesla Hub. No remote gateway is configured. If another VPN was previously active, reconnect it using its own app after stopping the test.

## Server says Running after its listener failed

Version 0.1.1 closes and removes listeners when their accept loop exits and retries binding on the next network scan. It also replaces the listener when an interface's subnet prefix changes, even if the IP address stays the same. The phone HTTP check runs when an address changes and every 15 seconds. Update the APK if the app version is still 0.1.0.

## Phone browser makes the Tesla connection indicator light up

Version 0.1.1 keeps phone browser reports separate from external browser reports. The Android status now says **Tesla / external browser**, because an external laptop or second phone is also a valid client. The phone browser's page still shows its own connection status and diagnostics.

## `teslahub.local` fails

Expected on some devices. Android NSD advertises a service, but does not guarantee this hostname or Tesla mDNS resolution. Use the numeric URL. The app reports NSD registration separately from hostname reachability.

## Browser reports EME, Widevine, or WebCodecs unavailable

The local launcher uses HTTP on a private IP. EME and WebCodecs may require HTTPS/secure context, so a missing API here is not conclusive for an external HTTPS provider page. Record the **Secure context** field. Do not change user agent or security settings to simulate DRM support.

## Stremio or RTP Play stays in normal browser mode

That is the supported Phase 1 fallback. A Tesla Theater redirect to arbitrary sites has not been verified and is not implemented. Provider playback also depends on Tesla browser codec support and each site's rules.

## Vodafone TV shows WF052 or black video

Record the exact error and the browser feature report. The Phase 1 diagnostics cannot distinguish all provider policy, OS checks, Widevine security level, and HDCP conditions. Protected video capture is not supported.

## Server stops when leaving Tesla Hub

Start it from the app and check that its foreground notification remains active. If ColorOS stops it, record battery optimization settings and the exact point it stopped. Do not rely on automatic Bluetooth start in Phase 1.
