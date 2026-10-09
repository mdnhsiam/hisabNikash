# HisabNikash

Bangla-first, offline-first personal income and expense tracking for Android. The first release includes quick entry, Cash/Bank/bKash/Nagad payment methods, custom categories, monthly dashboard/reports, optional budgets and savings goals, recurring reminders, encrypted phone-folder backup, and opt-in user-owned Google Drive backup.

## Install the closed-test build

1. Copy `app/build/outputs/apk/debug/app-debug.apk` to an Android 7.0 (API 24) or newer device.
2. Allow installation from the file manager when Android asks, then open the APK.
3. Because this is a debug-signed closed-test build, do not distribute it as a production Play Store release.
4. For Google Drive testing, register package `com.hisabnikash.app` and the test signing certificate in a Google Cloud OAuth client before connecting Drive.

Build locally with Android Studio's bundled JDK:

```powershell
.\gradlew.bat --no-daemon '-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr' clean testDebugUnitTest lintDebug assembleDebug
```

See the dated [release test report](docs/testing/release-test-report.md) for the candidate version, checksum and executed checks. The older `v0.1.0-alpha.1` GitHub prerelease is not the current hardened candidate.

See [production readiness](docs/production-readiness-2026-10-09.md), [manual-release-checklist.md](docs/testing/manual-release-checklist.md), and [data-handling.md](docs/privacy/data-handling.md). Real-device restore, Google authorization and owner-controlled release signing are required before production sign-off; a passing build does not establish those results.

## Release verification

```powershell
$env:GRADLE_USER_HOME='C:\Users\user\.gradle'
.\gradlew.bat --no-daemon '-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr' clean testDebugUnitTest lintDebug lintRelease compileDebugAndroidTestKotlin assembleDebug assembleRelease
```

`connectedDebugAndroidTest` additionally requires a booted Android device/emulator. The manual GitHub workflow runs these gates and API 24/36 instrumentation only after the owner confirms free Actions allowance and stopped paid overage. It has no automatic trigger and does not publish or sign an APK.

This native Android app does not need web hosting. Core use is offline and free; optional Drive backup uses each user's own Google account and available storage, not developer-operated servers. Drive backup is a snapshot/restore feature, not live multi-device sync.
