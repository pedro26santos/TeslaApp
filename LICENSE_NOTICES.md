# License and external code decisions

Phase 1 contains original Kotlin, HTML, CSS, and JavaScript written for this project. No Android Auto receiver source or commercial APK code has been copied. The Android SDK, Gradle, Kotlin compiler, and GitHub Actions are build tools, not vendored application code. This file records candidate components before any Phase 3 integration.

| Project | Stated license | Possible use | Decision |
|---|---|---|---|
| [Open Headunit / Headunit Revived](https://github.com/andreknieriem/open-headunit) | [AGPL-3.0](https://github.com/andreknieriem/open-headunit/blob/main/LICENSE), with [Michael Reid attribution](https://github.com/andreknieriem/open-headunit/blob/main/COPYRIGHT_MICHAEL_REID_GPLv3AFFERO.txt) | Android-native receiver and same-device Self-Mode research | Most relevant Phase 3 candidate, but **no code reused yet**. Any incorporation requires AGPL compatibility review, corresponding source availability, copyright and license notices, and preservation of upstream notices. Verify current Android Auto compatibility on the OPPO first. |
| [iConsole OpenHU](https://github.com/iConsole/OpenHU) | [AGPL-3.0](https://github.com/iConsole/OpenHU/blob/master/LICENSE) plus upstream and project notices | Historic Android receiver reference | Do not integrate: its README says current Google Play Services was unsupported at the time of that project. Preserve required notices if ever revisited. |
| [OpenAuto](https://github.com/f1xpl/openauto) and [aasdk](https://github.com/f1xpl/aasdk) | GPL-3.0 | Protocol and receiver architecture reference | Not used. Primarily C++/Linux head-unit components and a poor direct fit for this Kotlin Android app. Copying/linking would require GPL compatibility and notices. |
| [DiAuto](https://github.com/shihabal3amri/DiAuto) | AGPL-3.0 | Recent Android receiver fork | Not used. Vehicle-specific fork; evaluate upstream Open Headunit first. Preserve original and fork notices if integrated later. |

No Phase 1 runtime dependency on these projects exists. This is an engineering license inventory, not a grant to relicense upstream code. Before selecting a receiver, inspect the exact source revision and every transitive dependency; decide how the combined app source and license obligations will be fulfilled. Do not use paid/proprietary Castla PRO or Headunit Reloaded code.

## Local connection compatibility research

The public [TeslaMirror Android FAQ](https://teslamirror.com/faq-android) was read for its developer's description of private-address restrictions, local VPN routing, and HTTPS requirements. The website is copyrighted by Hustmobile Inc.; no open-source license for that product is assumed. No product code, APK contents, assets, certificates, or private keys are reused. Version 0.1.2's local route prototype is original code using Android's public `VpnService` and socket APIs, guided by [Android's API documentation](https://developer.android.com/develop/connectivity/vpn). It adds no third-party runtime library or commercial dependency.
