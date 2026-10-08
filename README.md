# Hisab

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

The final APK SHA-256 is recorded below after the release build:

`5e96ce61e54cd4033c15d217f529ebb29092ae00f15723a368a97891e78d8206`

See [manual-release-checklist.md](docs/testing/manual-release-checklist.md) for device checks and [data-handling.md](docs/privacy/data-handling.md) for privacy/security details.
