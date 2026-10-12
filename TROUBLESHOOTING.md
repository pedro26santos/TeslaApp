# Troubleshooting

## No fallback address

Enable the OPPO's **internet-sharing** Wi-Fi hotspot and start the server. Wait five seconds for interface discovery. If the app still says “No Wi-Fi/hotspot interface detected,” record the phone's Android and ColorOS versions, the hotspot screen status, and the app status text. The Phase 1 interface filter may need adjustment for ColorOS's interface name. No silent fallback to cellular or public binding is used.

## Tesla cannot open the numeric URL

Confirm the Tesla's Wi-Fi is connected to the OPPO hotspot, copy the exact fallback URL including `http://` and `:8765/`, and keep the Tesla Hub server notification active. Try each address listed under **All local addresses** if multiple are shown. A Tesla browser “site cannot be reached” result may mean hotspot client isolation, a different interface address, or ColorOS stopping the service. Record the Tesla Wi-Fi IP/gateway when visible and the exact browser error.

If the launcher opens in Chrome on the OPPO but the Tesla reports `ERR_CONNECTION_REFUSED`, the phone-side HTTP server is responding. It remains necessary to test incoming access through the hotspot. Follow Test 3 in [TESTING.md](TESTING.md). Restarting the hotspot can change the numeric URL, so copy it again. Version 0.1.1 distinguishes the phone's connected Wi-Fi interface from other local interface candidates and places hotspot candidates first. Interface names alone do not prove the hotspot role.

**Phone server check: Passed** verifies only the phone's HTTP response. **Connections from other devices** counts accepted TCP connections whose source is not the phone's own bound address, including requests rejected by the subnet check. If the count stays zero during the Tesla attempt, the app has not accepted a connection from the car. If it increases, include **Last request error** and **Copy connection diagnostics** so request rejection can be distinguished from a routing failure.

There are [reports of local-address connection failures in a Tesla-owned issue tracker](https://github.com/teslamotors/fleet-telemetry/issues/423), but the report does not establish the cause on this car. Do not assume a universal Tesla browser restriction without comparing another device on the same hotspot. A Tesla software version and that comparison are needed to investigate firmware-specific behavior.

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
