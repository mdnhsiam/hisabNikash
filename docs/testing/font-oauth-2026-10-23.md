# HisabNikash 0.1.6 font / OAuth follow-up

Date: 2026-10-23 (Asia/Dhaka). Version 0.1.6/code7. Base: `72c1ae17dd9478f87bf0733110f5374b2da43e39`.

## Scope

Owner-approved Hind Siliguri for display/headline/title/label roles, with packaged Noto Sans Bengali for explanatory body prose. Four real Hind weights (400/500/600/700) and OFL license downloaded from the official google/fonts repository; both licenses are reachable from More. Standalone amount labels in history/recent entries, budgets, savings, reports, duplicate review and recurring review explicitly use titleMedium (16sp/SemiBold). Native OS notifications and text-field input typography are unchanged. All fonts work offline; no provider/runtime dependency added.

No finance calculation, schema, stored ledger, encryption, network authorization implementation or signing configuration changed. Monthly totals remain visible, recent entries locally hidden, half/full entry sheet unchanged.

## Verification

Baseline Gradle test task succeeded before changes (cached/up-to-date). Observed RED then GREEN for distinct display/prose rendering. License dialog test observed RED before showing both family licenses; subsequent full run passed. Independent read-only review found default-body amount labels missed the approved contract. Actual history amount regression initially selected a merged card description; corrected to the unmerged amount Text node, observed RED on fontFamily, then applied explicit display styles. Final full gate and artifact details are recorded below after completion.

Independent rereview closed the Important finding: no Critical/Important or actionable Minor findings remain in the typography diff. This does not certify physical-device glyph clipping, large text, TalkBack or Cloud transport.

Final command (installed local Gradle cache and Android Studio JDK):

```
.\gradlew.bat --no-daemon "-Dorg.gradle.java.home=C:/Program Files/Android/Android Studio/jbr" testDebugUnitTest lintDebug lintRelease compileDebugAndroidTestKotlin assembleDebug assembleRelease --console=plain
```

Final result: BUILD SUCCESSFUL in 2m38s,129 tasks (32 executed,97 up-to-date). 177 JVM/Robolectric tests, zero failures. Debug and release lint: No issues found. Debug and minified unsigned release APK builds passed. Instrumentation sources compiled, not executed. No application code changed after this gate. Native Robolectric Home English, Bangla200% and expanded entry renders inspected; these are not physical-phone screenshots. Existing non-blocking kapt processor-option warnings remain.

Debug APK:17,499,712 bytes; SHA256 `fed54b5347d48be121b30834b974edfecea4a769d71346c59d74316239be419e`. Package `com.hisabnikash.app`, minAPI24/targetAPI36, version0.1.6/code7. apksigner verification succeeded; certificate matches0.1.5, SHA1 `1B:49:EA:24:C0:84:6D:6C:3E:A4:77:C0:00:8C:AB:56:13:45:77:FC`. Debug16K zip alignment check exit0. This is a debug-signed controlled test candidate, not an owner-signed production build.

Published [v0.1.6-alpha.1](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.6-alpha.1), release421265268, asset667271293, at `2026-10-22T22:30:15Z` (2026-10-23 Asia/Dhaka). Fresh GitHub metadata confirms draft=false, prerelease=true, asset uploaded,17,499,712 bytes and matching SHA256. Remote tag points to application source `16c5c53ccd60558dc6ba89a5557a796adadc414b`. Later documentation commits do not alter the binary. No older release/asset removed.

## Cloud setup

The owner personally accepted Google's policy and completed branding. After explicit approval, Android test client, one approved owner test account and only drive.appdata declaration were created and verified in `hisabnikash-512420`. External/Testing remains enabled, not public production. Billing stays unlinked, no card/trial/paid increase added. See [setup audit](setup-status-2026-10-23.md) for public client identity, scope, free-limit caveats and owner test instructions. No secret/client JSON added to the repository/APK.

No phone is connected. Real Google consent, backup creation, cancellation/revocation and restore/data-preservation remain unverified. Never uninstall/clear the owner's real ledger for testing. Production owner signing and device instrumentation remain open. No zero-bugs/security-certification claim is made.
