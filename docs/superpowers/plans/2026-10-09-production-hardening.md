# Production Readiness Hardening Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Repair production-impacting defects in the approved Android app, exercise its real persistence and UI boundaries, and publish an evidence-based readiness assessment without claiming unexecuted tests passed.

**Architecture:** Keep Kotlin/Compose, Room as local source of truth, and encrypted user-owned backup snapshots. Harden existing interfaces rather than introducing a backend or new product features. Account, signing, physical-device and manual accessibility gates remain explicit when their required external inputs are unavailable.

**Tech Stack:** Kotlin, Compose Material 3, Room, WorkManager, Android Keystore, Google Drive appDataFolder, JUnit/Robolectric/Android instrumentation, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-01-personal-finance-android-design.md`

## Global Constraints

- Native Android; minimum API 24, target/compile API 36; BDT with Bangla and English.
- Initial distribution: Free, closed Android distribution for up to 20 trusted devices.
- Offline-first operation; network access is not required for core features.
- Cloud backup is opt-in and user-owned; no developer-owned storage or analytics.
- Backups must be authenticated/encrypted; failed restore must not alter the existing ledger.
- No public Play Store launch, concurrent live-device sync, OCR or paid tiers.
- Do not weaken machine security, invent Google credentials, or generate an unrecoverable production signing identity.
- The user's prior instruction to proceed without intermediate questions applies to in-scope repairs; retain the approved architecture and record consequential decisions.

## Review Focus

- A stale or repeated reminder action must not charge/skip a different occurrence or crash the app.
- A failed edit must preserve user input, remain visibly retryable, and never report a successful write.
- Month rollover/resume must update all current-month views without requiring a new ledger write.
- Corrupt or semantically invalid backups must leave every table unchanged, including allocation/reversal relationships.
- Backup disable/delete/restore racing background work must not resurrect secrets or overwrite the newest snapshot.

## Task 1: Persistence and restore integrity

**Files:** repositories/DAOs under `app/src/main/java/com/hisabnikash/app/data/`, `backup/RestoreValidator.kt`, `domain/FinanceCalculator.kt`; tests `RoomRepositoryJvmIntegrationTest.kt`, `RestoreValidatorTest.kt`, migration tests and `FinanceCalculatorTest.kt`.

**Interfaces:** Consumes the existing Room tables/models. Produces validated repository writes and `RestoreValidator.validate(snapshot): ValidatedRestore` with cross-record checks; retain backup format compatibility.

- [ ] Write regression tests for category/type mismatch, unbounded imported money, invalid budget keys/slots, orphan/duplicate reversals, SQL metacharacters preserved as data, and full-table restore rollback.
- [ ] Run the tests before fixes; inspect individual assertion failures rather than compilation errors.
- [ ] Enforce model and category validity at repository and restore boundaries; use overflow-safe percentages and prevent aggregate overflow in restored data. Keep historical archived category labels.
- [ ] Exercise Room migrations on actual SQLite, including v1→v3 and v2→v3 and Bengali budget-key normalization.
- [x] Run the complete JVM suite and commit the independently testable repairs (98 tests, zero failures).

## Task 2: Reliable finance and entry/reminder UI

**Files:** `core/time/`, finance ViewModels, `navigation/HisabNavHost.kt`, transaction/recurring/savings/budget screens, both strings resource files; corresponding ViewModel and Compose tests.

**Interfaces:** Carry displayed `nextDueAtEpochMillis` from recurring screen/notification through repository confirmation and skip; return explicit action success/failure. Observe an injectable current-month flow across dashboard/budget/savings. Separate lifetime savings available for goals from automatic monthly savings.

- [ ] Write failing tests for stale confirmation/skip, safe failures, month rollover without a transaction emission, and monthly-vs-lifetime savings.
- [ ] Run RED; implement the smallest fixes and keep totals driven by persisted data.
- [ ] Write Compose regressions for failed-edit retention, changing type requiring an applicable category, optional field/date entry, complete recurring review and visible optional-form errors.
- [ ] Run RED; make long dialogs scrollable, disable duplicate submissions, preserve drafts on failure, and localize actionable messages.
- [ ] Run the complete JVM suite and instrumentation compilation, then commit.

## Task 3: Backup lifecycle and platform security

**Files:** `backup/local/`, `backup/drive/`, `backup/PreRestoreBackupStore.kt`, `feature/settings/`, `AppContainer.kt`, `data/preferences/`, extraction rules, manifest; backup/lifecycle/platform tests.

**Interfaces:** A shared suspend operation gate serializes backup/restore/disable/delete across local and Drive services. Obtain authorization for each manual Drive operation; no indefinitely reused in-memory token. SAF replacement preserves a recoverable previous snapshot until new-file promotion succeeds.

- [ ] Write deterministic concurrent-operation regressions and failed-provider replacement tests; run RED.
- [ ] Serialize backup lifecycle changes, fix stale-token behavior and retain secrets only while their destination is enabled.
- [ ] Validate private Android extraction exclusions for every data domain and privacy-safe notifications/intents.
- [ ] Run wrong-password/tamper/oversized-input/storage/network/auth tests, full suite and lint; commit.

## Task 4: Reproducible release gates and handoff

**Files:** `.github/workflows/`, release/security/testing documentation, Gradle release configuration only if needed; smoke/instrumentation tests.

**Interfaces:** CI consumes the checked-in wrapper/schema files and outputs test/lint evidence and unsigned release candidates. Production signing/OAuth configuration comes from the owner, never a committed secret.

- [ ] Run fresh clean JVM tests, lint, instrumentation compilation, debug and minified release builds.
- [ ] Run Android instrumentation/UI tests on an available emulator or connected device; if unavailable, record the exact limitation, not a pass.
- [ ] Add least-privilege CI using pinned official actions and an Android emulator gate; verify its real execution when GitHub permissions allow.
- [ ] Inspect final APK manifest/debuggability/permissions, dependency inventory and source secret/log scan.
- [ ] Obtain one fresh whole-diff review; fix Important/Critical findings with RED→GREEN regressions and a green suite.
- [ ] Update readiness/security evidence, manual checklist and installation instructions; commit and push normal non-force changes when verification is green.
- [ ] Publish a new private pre-release candidate only; do not relabel an unverified build production-ready.

## Gate commands

On Windows use the existing Gradle cache and Android Studio JDK:

`$env:GRADLE_USER_HOME='C:\Users\user\.gradle'; .\gradlew.bat --no-daemon '-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr' testDebugUnitTest lintDebug compileDebugAndroidTestKotlin assembleDebug assembleRelease`

Run `connectedDebugAndroidTest` separately with an available Android runtime. The final clean build is sequential to avoid concurrent Gradle/Room output races.

## Readiness interpretation

Passing automated checks is necessary, not sufficient. The spec explicitly disallows release sign-off while critical real-device restore paths are unverified. Report confirmed repairs, exact test counts/results, independent review findings and remaining owner/device gates. Never claim zero bugs, complete penetration-test coverage, or compliance certification.
