# HisabNikash

<img src="app/src/main/res/drawable-nodpi/hisab_logo.png" width="96" alt="HisabNikash ledger and checkmark logo">

Bangla-first, offline-first personal income and expense tracking for Android. The first release includes quick entry, Cash/Bank/bKash/Nagad payment methods, custom categories, monthly dashboard/reports, optional budgets and savings goals, recurring reminders, encrypted phone-folder backup, and opt-in user-owned Google Drive backup.

The generated ledger/checkmark logo is placed in the dashboard and Android launcher. [Logo source, generation prompts and placement](docs/branding/logo-generation.md), [branding review and regression evidence](docs/branding/review-and-verification.md).

## Current published test build: 0.1.6

The owner-approved follow-up packages Hind Siliguri for display/headings/buttons and emphasized financial amounts, with Noto Sans Bengali for body prose. Both fonts and their licenses work offline. Monthly totals stay visible, recent entries locally hidden, More shortcuts and half/full native entry sheet unchanged. Owner-approved Google Cloud test configuration is ready, but phone authorization and actual backup/restore remain unverified. No billing account/card/paid quota increase was added. [Setup audit and Google OAuth runbook](docs/testing/setup-status-2026-10-23.md).

Download [HisabNikash 0.1.6 test APK](https://github.com/mdnhsiam/hisabNikash/releases/download/v0.1.6-alpha.1/HisabNikash-0.1.6-test.apk) from the private [0.1.6 prerelease](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.6-alpha.1), published2026-10-23 (Asia/Dhaka). Remote size/SHA256 match the verified local APK. Install over the existing app without uninstall/data clear; actual phone update/data preservation remains unverified. [177-test verification and limitations](docs/testing/font-oauth-2026-10-23.md). This is not production approval. Previous [0.1.5 prerelease](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.5-alpha.1) and its [174-test report](docs/testing/ui-polish-2026-10-23.md) remain historical evidence.

## Previous published closed-test build: 0.1.4

The redesigned native UI is implemented and pushed: private-by-default Home with eye-to-reveal, readable offline Bangla typography, four navigation destinations and an expandable transaction sheet. Clean verification:171 tests, zero failures; debug/release lint zero issues. [UI review](docs/testing/minimal-ui-review-2026-10-21.md).

Download [HisabNikash 0.1.4 test APK](https://github.com/mdnhsiam/hisabNikash/releases/download/v0.1.4-alpha.1/HisabNikash-0.1.4-test.apk) from the private [0.1.4 prerelease](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.4-alpha.1). Published on 2026-10-22 (Asia/Dhaka); GitHub's asset size and SHA256 match the verified local binary. Sign into a GitHub account with repository access. See the [exact artifact/checksum report](docs/testing/release-test-report.md). Install as an update without uninstalling or clearing data. This is a debug-signed test build, not production sign-off.

## Previous published closed-test build: 0.1.3

Download [HisabNikash 0.1.3 test APK](https://github.com/mdnhsiam/hisabNikash/releases/download/v0.1.3-alpha.1/HisabNikash-0.1.3-test.apk) from the private [test release](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.3-alpha.1). Sign into a GitHub account with repository access. This replacement fixes a reproduced cold-start crash; it is not production sign-off. Install over the existing app without uninstalling or clearing data. If Android rejects the update, stop rather than uninstalling unbacked-up data. Physical-phone launch/update verification remains outstanding. The old0.1.2 APK was withdrawn; its release/tag/history remain.

1. Copy `app/build/outputs/apk/debug/app-debug.apk` to an Android 7.0 (API 24) or newer device.
2. Allow installation from the file manager when Android asks, then open the APK.
3. Because this is a debug-signed closed-test build, do not distribute it as a production Play Store release.
4. For Google Drive testing, register package `com.hisabnikash.app` and the test signing certificate in a Google Cloud OAuth client before connecting Drive.

Selecting a folder or connecting Drive is only setup: use **Back up now** with a password to create the first encrypted copy. Settings distinguishes incomplete setup from a successfully configured backup. Keep the password somewhere safe; this app cannot recover a forgotten one.

A future owner-signed release may use a different certificate from this debug build. Android will not accept it as an in-place update. Verify an external backup and its restore/password before uninstalling, then restore after installing the owner-signed build; never uninstall unbacked-up real data.

Build locally with Android Studio's bundled JDK:

```powershell
.\gradlew.bat --no-daemon '-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr' clean testDebugUnitTest lintDebug assembleDebug
```

See the dated [release test report](docs/testing/release-test-report.md) for version, checksum and executed checks. The current published test build is `v0.1.4-alpha.1`; older0.1.0/withdrawn0.1.2 and previous0.1.3 builds are not current candidates.

See [production readiness](docs/production-readiness-2026-10-09.md), [manual-release-checklist.md](docs/testing/manual-release-checklist.md), and [data-handling.md](docs/privacy/data-handling.md). Real-device restore, Google authorization and owner-controlled release signing are required before production sign-off; a passing build does not establish those results.

## Release verification

```powershell
$env:GRADLE_USER_HOME='C:\Users\user\.gradle'
.\gradlew.bat --no-daemon '-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr' clean testDebugUnitTest lintDebug lintRelease compileDebugAndroidTestKotlin assembleDebug assembleRelease
```

`connectedDebugAndroidTest` additionally requires a booted Android device/emulator. The manual GitHub workflow runs these gates and API 24/36 instrumentation only after the owner confirms free Actions allowance and stopped paid overage. It has no automatic trigger and does not publish or sign an APK.

This native Android app does not need web hosting. Core use is offline and free; optional Drive backup uses each user's own Google account and available storage, not developer-operated servers. Drive backup is a snapshot/restore feature, not live multi-device sync.
