# License and external code decisions

Phase 1 contains original Kotlin, HTML, CSS, and JavaScript written for this project. The Android SDK, Gradle, Kotlin compiler, and GitHub Actions are build tools, not vendored application code. Phase 3 adds a separately built Open Headunit prototype, with its source patch and notices described below. No commercial APK code is used.

| Project | Stated license | Possible use | Decision |
|---|---|---|---|
| [Open Headunit / Headunit Revived](https://github.com/andreknieriem/open-headunit) | AGPL-3.0-or-later, with Michael Reid and upstream attribution | Separate Android-native receiver for Phase 3 qualification | Source revision **4ee1d58f6cf7f39f7dc0a6ef93509553401ec6aa** inspected and built as a separate local APK. [Patch, licenses and build recipe](third_party/open-headunit/README.md) retained. Tesla Hub's own APK contains an original intent/JSON client, not the receiver implementation or contract library. Real OPPO projection, touch, stop/reconnect and local VPN coexistence passed; parked-car audio/calls remain qualification gates. |
| [iConsole OpenHU](https://github.com/iConsole/OpenHU) | [AGPL-3.0](https://github.com/iConsole/OpenHU/blob/master/LICENSE) plus upstream and project notices | Historic Android receiver reference | Do not integrate: its README says current Google Play Services was unsupported at the time of that project. Preserve required notices if ever revisited. |
| [OpenAuto](https://github.com/f1xpl/openauto) and [aasdk](https://github.com/f1xpl/aasdk) | GPL-3.0 | Protocol and receiver architecture reference | Not used. Primarily C++/Linux head-unit components and a poor direct fit for this Kotlin Android app. Copying/linking would require GPL compatibility and notices. |
| [DiAuto](https://github.com/shihabal3amri/DiAuto) | AGPL-3.0 | Recent Android receiver fork | Not used. Vehicle-specific fork; evaluate upstream Open Headunit first. Preserve original and fork notices if integrated later. |

No Phase 1 runtime dependency on these projects exists. This is an engineering license inventory, not a grant to relicense upstream code. Before selecting a receiver, inspect the exact source revision and every transitive dependency; decide how the combined app source and license obligations will be fulfilled. Do not use paid/proprietary Castla PRO or Headunit Reloaded code.

## Local connection compatibility research

The public [TeslaMirror Android FAQ](https://teslamirror.com/faq-android) was read for its developer's description of private-address restrictions, local VPN routing, and HTTPS requirements. The website is copyrighted by Hustmobile Inc.; no open-source license for that product is assumed. No product code, APK contents, assets, certificates, or private keys are reused. Version 0.1.2's local route prototype is original code using Android's public `VpnService` and socket APIs, guided by [Android's API documentation](https://developer.android.com/develop/connectivity/vpn). It adds no third-party runtime library or commercial dependency.

Phase 2 uses original Kotlin with Android framework Bluetooth, settings, notification, and preference APIs. No new third-party runtime dependency is added. MacroDroid's public documentation was consulted for optional user setup only; no MacroDroid code, helper APK, or proprietary asset is included.

## Phase 3 receiver source build

The source-built **com.teslahub.receiver** prototype preserves Open Headunit's AGPL source, assets and license. Its modifications and original instrumentation follow AGPL-3.0-or-later. Corresponding inspected source is available to the owner at `.tooling/receiver-source`; the pinned revision and [receiver.patch](third_party/open-headunit/receiver.patch) record the reproducible changes. Full AGPL text and Michael Reid's upstream notice are preserved under `third_party/open-headunit`. The modified version/package distinguishes this local prototype from the official receiver. No upstream signing keystore, protocol key, compiled library or APK is added to Git.

The receiver's transitive Gradle/native inventory and remaining native-source/redistribution notice checks are in [third_party/open-headunit/README.md](third_party/open-headunit/README.md). Its FFmpeg 7.1.1 binary identifies LGPL-2.1-or-later and a configuration without GPL enablement. Existing GPL/LGPL and other artifact notices must remain with any future receiver redistribution. This phase builds and installs a local qualification APK; it does not publish that binary. Linking receiver code into Tesla Hub or distributing browser projection would require a further combined-source and corresponding-source review.
