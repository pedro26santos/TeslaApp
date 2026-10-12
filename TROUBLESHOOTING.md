# Troubleshooting

## No fallback address

Enable the OPPO's **internet-sharing** Wi-Fi hotspot and start the server. Wait five seconds for interface discovery. If the app still says “No Wi-Fi/hotspot interface detected,” record the phone's Android and ColorOS versions, the hotspot screen status, and the app status text. The Phase 1 interface filter may need adjustment for ColorOS's interface name. No silent fallback to cellular or public binding is used.

## Tesla cannot open the numeric URL

Confirm the Tesla's Wi-Fi is connected to the OPPO hotspot, copy the exact fallback URL including `http://` and `:8765/`, and keep the Tesla Hub server notification active. Try each address listed under **All local addresses** if multiple are shown. A Tesla browser “site cannot be reached” result may mean hotspot client isolation, a different interface address, or ColorOS stopping the service. Record the Tesla Wi-Fi IP/gateway when visible and the exact browser error.

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
