# Closed-test build report

Date: 2026-10-05 (Asia/Dhaka)

## Automated results

- `testDebugUnitTest`: 68 tests, 0 failures.
- `compileDebugAndroidTestKotlin`: passed; instrumentation sources compile.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- `assembleRelease`: passed, including R8 minification and release lint-vital checks. The release output is unsigned; the distributable closed-test artifact is the debug-signed APK.
- Sensitive log/embedded-secret source scan: clean.

The exact combined command with `connectedDebugAndroidTest` built both test APKs and then stopped with `No connected devices!`. This is recorded as unavailable, not passing. Real-device and OAuth checks remain listed in the manual checklist.

## Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 15,764,359 bytes
- SHA-256: `f3d896139ef15fc05820603fbef053659f243fef335f20ea390964fdc4a9b96e`
