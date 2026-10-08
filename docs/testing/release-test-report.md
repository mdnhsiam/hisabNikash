# Closed-test build report

Date: 2026-10-09 (Asia/Dhaka)

## Automated results

- `testDebugUnitTest`: 76 tests, 0 failures.
- `compileDebugAndroidTestKotlin`: passed; instrumentation sources compile.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- `assembleRelease`: passed, including R8 minification and release lint-vital checks. The release output is unsigned; the distributable closed-test artifact is the debug-signed APK.
- Sensitive log/embedded-secret source scan: clean.

The exact combined command with `connectedDebugAndroidTest` built both test APKs and then stopped with `No connected devices!`. This is recorded as unavailable, not passing. Real-device and OAuth checks remain listed in the manual checklist.

## Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 15,836,767 bytes
- SHA-256: `5e96ce61e54cd4033c15d217f529ebb29092ae00f15723a368a97891e78d8206`
