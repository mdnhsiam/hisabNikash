# Closed-test build report

Date: 2026-10-11 (Asia/Dhaka). Candidate: HisabNikash 0.1.2, version code 3. Verified application source: `db3d9e76acd266aec5613cf8838c98b56bb7e96e`.

## Automated results

- Clean `testDebugUnitTest`: 46 suites, 157 tests, 0 failures/errors/skips. Includes real SQLite migrations/rollback, finance ViewModels, Compose interactions, encrypted backups, concurrency, provider-failure, all four core final-review regressions and six branding tests.
- `compileDebugAndroidTestKotlin`: passed; instrumentation sources compile.
- `lintDebug` and `lintRelease`: passed, both XML reports contain 0 issues.
- `assembleDebug`: passed.
- `assembleRelease`: passed, including R8 minification; release lint was run separately with `lintRelease` (the redundant lint-vital tasks were skipped). The release output is unsigned; the closed-test artifact is the debug-signed APK.
- Sensitive log/embedded-secret source scan: clean.
- Runtime inventory/advisory check: 107 resolved release runtime Maven packages, 0 differences from the reviewed inventory; the [2026-10-11 OSV record](../security/osv-runtime-2026-10-11.json) returned 0 known affected packages. This is not a no-vulnerabilities guarantee.
- APK inspection: package/version/minimum API 24/target API 36 correct; debug signature verifies (v2), both APKs pass 16K zip alignment, release has no debuggable flag and is unsigned as expected.

The final branded-candidate clean gate succeeded in 2m57s (131 tasks). Non-blocking kapt processor-option warnings remain in the compiler log; neither lint report has a finding. No source changes were made after this run except release documentation.

A fresh whole-branch review found four Important issues and no Critical/Minor findings. All four received failing regressions followed by repairs; the complete final suite is green. See the [review/fix record](production-hardening-review.md). No second review or penetration-test sign-off is claimed.

A separate branding-only review found one Important circular safe-area issue and no Critical/Minor findings. It was repaired with a real-resource rendering RED→GREEN regression. The generated mark is placed in the dashboard and legacy/adaptive/themed launcher resources; the Bangla identity test uses 200% text size. Native Robolectric dashboard/launcher renders were visually inspected, not represented as phone screenshots. See the [branding review record](../branding/review-and-verification.md).

The earlier core candidate's combined command with `connectedDebugAndroidTest` built both test APKs and stopped with `No connected devices!`. The current candidate's instrumentation compiles; fresh `adb devices -l` is still empty and instrumentation was not executed for this branding build. This is recorded as unavailable, not passing. Real-device and OAuth checks remain listed in the manual checklist.

## Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: 16,459,283 bytes
- SHA-256: `47b3c541aefb9a61e567bf427821ce9fdae5a83908c80d58d21579df50b859be`
- Public debug signing certificate SHA-1: `1b49ea24c0846d6c3ea477c0008cab56134577fc` (for owner-configured test OAuth only; not a production certificate).

The current APK was published on 2026-10-11 (Asia/Dhaka) in the private [v0.1.2-alpha.1 test release](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.2-alpha.1), tagged at `16cc5c47b4b4669a9c2017dbe9d80b6c0c9272f7`. [Download HisabNikash-0.1.2-test.apk](https://github.com/mdnhsiam/hisabNikash/releases/download/v0.1.2-alpha.1/HisabNikash-0.1.2-test.apk). The connected Chrome upload route was available again; the published release page shows the correct commit, pre-release label, asset name and SHA-256 matching the local verified APK. This is binary publication for controlled testing, not production deployment. The older `v0.1.0-alpha.1` asset remains intact and is not this build. Do not commit generated APKs or signing secrets to source control.

See [production readiness](../production-readiness-2026-10-09.md) for the separate real-device, authorization and signing gates. Automated evidence is not production sign-off.
