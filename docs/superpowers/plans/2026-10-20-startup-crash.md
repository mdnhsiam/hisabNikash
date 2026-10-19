# Startup Crash Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair the reproduced cold-start navigation crash and provide an update-compatible private APK.

**Architecture:** Preserve `Route.bottomRoutes: List<Route>` but defer its construction until after route singleton initialization. Tests exercise real compiled route classes in independent class loaders and the real Application/Activity/navigation/container path.

**Tech Stack:** Kotlin/Compose, JUnit, Robolectric, Android instrumentation, Gradle.

**Spec:** `docs/testing/startup-crash-2026-10-20.md` (user approved implementation/publication; prior instruction waives intermediate plan questions).

## Global Constraints

- Minimum API24, target API36; no dependency/schema/storage changes.
- Preserve package `com.hisabnikash.app` and signing certificate; version0.1.3/code4.
- No phone data clearing/uninstall; no production claims or paid CI.
- New private pre-release `v0.1.3-alpha.1`; withdraw only the previous0.1.2 APK download after replacement verification, respecting action-time confirmation.

## Review Focus

- Any one of the five singleton routes may initialize first: a fresh class-loader case for each must return the five expected paths in order.
- A warm test JVM must not hide null initialization: use independent class loading, including parent-first control.
- API36 launch must reach the real dashboard and survive recreation: real MainActivity Robolectric launch plus device-test source.
- API24 minimum-OS launch must reach the dashboard: legacy-graphics Robolectric launch.
- Update must not wipe the ledger: no database/preference changes, same package/certificate and higher version; actual phone update remains an explicit unverified gate.

### Task 1: Repair and verify cold startup

**Files:** Modify `app/src/main/java/com/hisabnikash/app/navigation/Route.kt`, `app/build.gradle.kts`; create `app/src/test/java/com/hisabnikash/app/navigation/RouteInitializationTest.kt`, `app/src/test/java/com/hisabnikash/app/AppStartupTest.kt`, `app/src/androidTest/java/com/hisabnikash/app/AppStartupInstrumentedTest.kt`; update release reports/README.

**Interfaces:** Consumes existing `Route` singleton objects and real `MainActivity`. Produces unchanged `bottomRoutes: List<Route>` and an update-compatible0.1.3 APK.

- [x] Step1: Add class-loader-isolated tests: each of Dashboard/Transactions/Reports/Savings/Settings first, and parent-first control; assert literal ordered paths `[dashboard,transactions,reports,savings,settings]` and no nulls.
- [x] Step2: Promote full-startup diagnostic into normal tests on API35/36 and add API24 legacy-graphics case; assert localized dashboard and navigation labels, then Activity recreation. Supply only the real WorkManager manifest initializer in Robolectric. Add equivalent device-test source without faking production dependencies.
- [x] Step3: Run focused tests on unchanged production code. Expected: startup and singleton-first cases fail from null route; parent-first control succeeds. Keep RED evidence.
- [x] Step4: Change `Route.bottomRoutes` to deferred construction and bump version to0.1.3/code4; no unrelated refactors. Focused tests must pass.
- [x] Step5: Run clean whole suite, debug/release lint, instrumentation compilation and both APK builds. Expected: exit0, no test failures/lint issues; report compiler warnings honestly. Verify APK signature/certificate/version/checksum/16K alignment.
- [x] Step6: Commit verified source/tests and record reviewable evidence; independent read-only review before push. Fix any Critical/Important issue with RED→GREEN and a green full suite.
- [ ] Step7: Push source; upload exact verified APK as a private pre-release and verify GitHub digest. Update download/report documentation, withdraw previous0.1.2 asset after required confirmation; retain history/local copy. No claim of physical-device verification without execution.
