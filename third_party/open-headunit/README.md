# Phase 3 receiver prototype

Upstream: https://github.com/andreknieriem/open-headunit

Pinned source revision: **4ee1d58f6cf7f39f7dc0a6ef93509553401ec6aa**.

Upstream version: 3.5.0-beta4. This local prototype is **3.5.0-beta4-teslahub1**, application ID **com.teslahub.receiver**. It is a separate APK and process. Tesla Hub uses explicit Android intents and bounded JSON replies; it does not link or copy the receiver implementation into its APK. The receiver is a modified Open Headunit build, not an official upstream release.

## Source and notices

The receiver source is licensed under **AGPL-3.0-or-later**. Preserve [LICENSE](LICENSE) and [Michael Reid's copyright notice](COPYRIGHT_MICHAEL_REID_GPLv3AFFERO.txt). The supplied [receiver.patch](receiver.patch), including the new diagnostics code, follows the receiver's AGPL terms. It is a patch to the pinned revision; it is not a grant to relicense upstream code.

Complete inspected and modified source is available locally at `.tooling/receiver-source`, with its Git history and upstream notices. The pinned upstream revision plus this patch reproduce the source changes. No signing keys, upstream keystore, compiled libraries, APKs, or proprietary receiver code are copied into TeslaApp's Git repository.

## Changes made on 13 October 2026

- Separate application ID and version label; update the receiver's internal package constant accordingly.
- Kotlin 2.2.20 to match the installed AGP/compiler combination and avoid the newer Kotlin metadata warnings.
- First-test defaults: headunit-server mode, H.264, 720p, 30 FPS, and phone microphone rather than the headunit microphone.
- Extend the existing query-state reply with a versioned, bounded diagnostic object: transport started, frames rendered, actual decoded dimensions, advertised dimensions/DPI/FPS, codec, last-frame age, and configured audio sink codecs. No framebuffer, microphone samples, account data, phone identity, or network credentials are exported.
- Configured audio sinks and advertised frame rate are explicitly distinct from proof of audible playback or a measured frame rate.

The source folder is named `receiver-source`: the installed Windows AIDL tool emits raw file paths in Java comments, and a lowercase `\u` directory segment in the former folder name caused an illegal Unicode escape. This build-folder change requires no receiver protocol change.

## Build

With this workspace's JDK and SDK installed:

```powershell
.\tools\Build-Receiver.ps1
```

After dependencies are cached, use `-Offline`. The build uses Gradle 8.14.4, AGP 8.13.2, platform 36, NDK 29.0.14206865, CMake 3.22.1, and only the OPPO's arm64-v8a ABI. The first build downloads public dependencies and SDK components. Existing source at another revision or an unapplicable patch is preserved and reported as an error.

## Dependency inventory for this private qualification build

The receiver's Gradle file also uses AndroidX/Material, protobuf, Glide, ZXing, Conscrypt, Google Play Services Nearby, DexMaker, Shizuku API, and libsu. Preserve their artifact notices and applicable upstream terms when redistributing a receiver package. No Shizuku/root functionality is enabled by this test, and the selected localhost path uses neither Native AA hotspot control nor a dummy VPN on Android Auto 17.4+.

Its arm64 native bundle includes libusb and an FFmpeg 7.1.1 HEVC decoder. The inspected FFmpeg binary identifies **LGPL-2.1-or-later** and a build without `--enable-gpl`; the upstream source tree supplies `FFMPEG_LICENSE.md` and `COPYING.LGPLv2.1`. The bundled HEVC decoder is not the H.264 hardware decoder selected for this test. Native corresponding-source/build provenance and the full redistribution notice bundle must be checked before distributing the receiver APK beyond this owner's local qualification setup. The APK is not published or committed by this prototype.

The app's AGPL notices are retained in its assets and the full cloned source remains available to the owner. The integration's public automation protocol can be inspected in the pinned upstream `contract/README.md` and `automation/AutomationEffectRunner.kt`.
