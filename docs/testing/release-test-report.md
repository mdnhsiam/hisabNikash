# Closed-test build report

Date:2026-10-20 (Asia/Dhaka). Candidate:HisabNikash0.1.3/code4. Verified application source and release tag:`41a2b5976ed095fd00e7f0363f99c58f625d3380`. Later documentation commits do not change the binary.

## Startup repair and verification

The eager navigation singleton list captured a null destination during cold initialization. Deferred construction repairs the reproduced crash without changing ledger/schema/preferences, dependencies, package or signing configuration. [Diagnosis/review rulings](startup-crash-2026-10-20.md).

- Focused regressions:RED5tests/4failures from null navigation; parent-first control passed. After repair:GREEN5/5.
- Complete clean suite:48suites,162tests,zero failures/errors/skips. Includes finance/SQLite/backup/branding coverage, real Application/MainActivity modeled API24/35/36 launch, API35/36 recreation, and each destination-first order in fresh class loaders.
- Debug/release lint:passed,zero issues.
- Instrumentation Kotlin compilation and test APK assembly:passed; Android tests NOT executed.
- Debug and R8-minified unsigned release APK builds:passed.
- Full clean gate with runtime inventory:BUILD SUCCESSFUL4m13s,149tasks (147executed,2up-to-date). Non-blocking kapt processor-option warnings remain. Release lint ran separately; redundant lint-vital tasks skipped.
- Targeted logging/embedded-secret filename scans:no matches; not a full security audit.
- Runtime:107selected Maven packages,0differences from reviewed inventory. [OSV refresh](../security/osv-runtime-2026-10-20.json) returned0known affected; not proof of no vulnerabilities. Snapshot SHA256:`a73eb28743bb72705cffd39667220c57441bf3eb69bbd2ded0a8d9fd0184fd29`.
- APK:package `com.hisabnikash.app`,0.1.3/code4,minAPI24,targetAPI36. Debug v2 signature verifies; same certificate as0.1.2. Both APKs pass16K zip alignment. Release non-debuggable and unsigned as expected.
- Independent scoped read-only review:ready to merge source repair; no Critical/Important/Minor findings. No production certification.

No implementation changes followed the clean gate. Fresh device inventory was empty; emulator acceleration reports no installed hypervisor driver. No physical Nothing Phone3a launch, instrumentation execution or actual installed update/ledger preservation is claimed.

## Published artifact

- [Private replacement release](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.3-alpha.1)
- [Download HisabNikash-0.1.3-test.apk](https://github.com/mdnhsiam/hisabNikash/releases/download/v0.1.3-alpha.1/HisabNikash-0.1.3-test.apk)
- Local build:`app/build/outputs/apk/debug/app-debug.apk`
- Size:16,459,283bytes
- SHA256:`bad86f384119a042b1469fa975ce61606eba10d57ad1ce77749117ec7eed3faf`
- Public debug certificate SHA256:`2db8c68906df844b71c7c03d186b5a8916f880548033d6d693fa0aa1610078c2`
- Public debug certificate SHA1:`1b49ea24c0846d6c3ea477c0008cab56134577fc` (test OAuth only).

GitHub asset657557786 reports matching digest/size. Published pre-release, not production deployment. Install over the existing app without uninstall/data clearing. Matching package/certificate and higher code support intended compatibility, not an installed-phone result.

Following explicit action-time owner confirmation, only asset628873383 (`HisabNikash-0.1.2-test.apk`) was deleted from release `v0.1.2-alpha.1`, now marked WITHDRAWN. GitHub metadata confirms its assets list is empty. Tag/release/source history remain, as does local rollback copy `C:/Users/user/Documents/new Try/HisabNikash-0.1.2-test.apk`. GitHub has no restore button, but the local file could be re-uploaded.0.1.0 was not touched. Historical0.1.2 report remains in Git history.

Real-device, actual folder/Drive restore, OAuth and owner signing gates remain open in the [manual checklist](manual-release-checklist.md) and [readiness assessment](../production-readiness-2026-10-09.md). Automated success does not establish zero defects.
