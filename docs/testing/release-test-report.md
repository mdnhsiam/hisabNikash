# Closed-test build report

Date: 2026-10-10 (Asia/Dhaka). Candidate: HisabNikash 0.1.1, version code 2. Verified application source: `14bdfb8e911a0422cd48f72a5bea7f37e2433674`.

## Automated results

- Clean `testDebugUnitTest`: 44 suites, 151 tests, 0 failures/errors/skips. Includes real SQLite migrations/rollback, finance ViewModels, Compose interactions, encrypted backups, concurrency, provider-failure and all four final-review regressions.
- `compileDebugAndroidTestKotlin`: passed; instrumentation sources compile.
- `lintDebug` and `lintRelease`: passed, both XML reports contain 0 issues.
- `assembleDebug`: passed.
- `assembleRelease`: passed, including R8 minification; release lint was run separately with `lintRelease` (the redundant lint-vital tasks were skipped). The release output is unsigned; the closed-test artifact is the debug-signed APK.
- Sensitive log/embedded-secret source scan: clean.
- Runtime inventory/advisory check: 107 resolved release runtime Maven packages, 0 differences from the reviewed inventory; refreshed OSV request returned 0 known affected packages. This is not a no-vulnerabilities guarantee.
- APK inspection: package/version/minimum API 24/target API 36 correct; debug signature verifies (v2), both APKs pass 16K zip alignment, release has no debuggable flag and is unsigned as expected.

The initial clean attempt exposed a Compose StateFlow read lint error, which was corrected. After the final review fix pass, the complete clean gate succeeded in 3m22s (130 tasks). Non-blocking kapt processor-option warnings remain in the compiler log; neither lint report has a finding.

A fresh whole-branch review found four Important issues and no Critical/Minor findings. All four received failing regressions followed by repairs; the complete final suite is green. See the [review/fix record](production-hardening-review.md). No second review or penetration-test sign-off is claimed.

The exact combined command with `connectedDebugAndroidTest` built both test APKs and then stopped with `No connected devices!`. This is recorded as unavailable, not passing. Real-device and OAuth checks remain listed in the manual checklist.

## Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 15,912,235 bytes
- SHA-256: `6702b6e1b38b16d859026c97b96b2a1e584b0679f8fd7fbf1475f76678f594c5`
- Public debug signing certificate SHA-1: `1b49ea24c0846d6c3ea477c0008cab56134577fc` (for owner-configured test OAuth only; not a production certificate).

The older GitHub `v0.1.0-alpha.1` asset is not this build. No new asset publication is claimed: the browser runtime failed to initialize and the connected GitHub tools do not expose release-asset upload. The current candidate is available in the local output path; do not commit generated APKs or signing secrets to source control.

See [production readiness](../production-readiness-2026-10-09.md) for the separate real-device, authorization and signing gates. Automated evidence is not production sign-off.
