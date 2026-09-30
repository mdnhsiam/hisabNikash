# Personal Finance Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a free, native, offline-first Android personal-finance app for Bangladesh with fast transaction entry, automatic dashboards and reports, optional budgets and savings goals, recurring reminders, and encrypted user-owned backup and restore.

**Architecture:** A single Android app uses Room as the local source of truth, repository interfaces around focused feature units, immutable `StateFlow` UI state, and Jetpack Compose screens. WorkManager handles reminders and queued backups; encrypted versioned snapshots go either through Android's Storage Access Framework or the Google Drive REST API using user-granted `drive.appdata` authorization. There is no developer-operated server and no concurrent live multi-device editing.

**Tech Stack:** Kotlin 2.2.20, Android Gradle Plugin 8.13.2, Gradle 8.13, Java 17 bytecode, compile/target SDK 36, minimum SDK 24, Jetpack Compose BOM 2026.03.00, Material 3, Room 2.8.5, DataStore 1.2.1, WorkManager 2.11.2, Navigation Compose 2.9.7, Google Play Services Auth 22.0.0, kotlinx.serialization, JUnit, kotlinx-coroutines-test, Room testing, Compose UI testing, and AndroidX Test.

**Spec:** `docs/superpowers/specs/2026-10-01-personal-finance-android-design.md`

## Global Constraints

- Working product name and package: **Hisab** / `com.hisabnikash.app`; changing branding later must not change persisted IDs or backup schema.
- Native Android only; `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`; Java/Kotlin bytecode target 17.
- BDT amounts are stored as signed `Long` poisha values, never floating point; accepted entry range is `0.01` through `999,999,999,999.99` BDT.
- A possible duplicate is the same type, amount, normalized description, category, and payment method saved within 60 seconds; the user can explicitly save it anyway.
- Core features work without account or network; Room is always the local source of truth.
- Bangla and English are complete localizations; no user-visible hard-coded strings in Kotlin.
- No analytics, advertisements, paid tier, developer-owned storage, OCR, financial-provider ingestion, or live concurrent device sync.
- Backups are versioned, authenticated AES-256-GCM payloads; passwords are processed with PBKDF2-HMAC-SHA256 and are never stored in a backup.
- Google Drive access requests only `https://www.googleapis.com/auth/drive.appdata`; no broad Drive scope.
- Production code never logs amounts, descriptions, tokens, passwords, keys, or decrypted backup data.
- Every task follows red-green-refactor TDD and ends with a focused commit.

## Review Focus

- Bangla digits, decimal input, zero/negative amounts, and values above the maximum must parse deterministically or show validation; Task 2 owns these tests.
- Transactions at Bangladesh month boundaries and daylight-saving-independent UTC instants must land in the correct `Asia/Dhaka` month; Task 4 owns these tests.
- Rapid duplicate submission must warn without blocking an intentional duplicate; Task 3 owns these tests.
- Archived custom categories must remain readable on historical transactions while disappearing from new-entry choices; Task 2 owns these tests.
- Wrong passwords, modified ciphertext, unsupported schema versions, and failed restores must leave the current database byte-for-byte logically unchanged; Task 9 owns these tests.

---

## File Structure

```text
app/src/main/java/com/hisabnikash/app/
  HisabApplication.kt                 application-owned AppContainer
  MainActivity.kt                     Compose host and activity-result bridges
  AppContainer.kt                     manual dependency graph
  core/model/                          domain enums and immutable models
  core/money/MoneyParser.kt           BDT/Bangla digit parsing and formatting
  core/time/MonthKey.kt               Asia/Dhaka month boundaries
  data/db/                             Room database, entities, DAOs, converters
  data/repository/                     repository interfaces and Room implementations
  data/preferences/UserPreferences.kt DataStore language, defaults, backup settings
  domain/FinanceCalculator.kt          dashboard/report calculations
  feature/dashboard/                   dashboard state, ViewModel, and Compose UI
  feature/transactions/                Quick Entry, list, edit, duplicate warning
  feature/budgets/                     total/category budget editing
  feature/savings/                     automatic savings and goal allocations
  feature/reports/                     monthly/category/payment-method reports
  feature/recurring/                   templates, scheduling, notification actions
  feature/settings/                    language, backup, restore, deletion
  backup/                              snapshot, crypto, SAF, Drive REST, workers
  navigation/HisabNavHost.kt           routes and bottom navigation
  ui/theme/                            colors, typography, spacing, components
app/src/main/res/values*/              localized strings, app metadata
app/src/test/                           JVM unit and repository tests
app/src/androidTest/                    Room, Compose, worker, restore tests
```

### Task 1: Reproducible Android shell and navigation

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`
- Create: `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/hisabnikash/app/{HisabApplication.kt,MainActivity.kt,AppContainer.kt}`
- Create: `app/src/main/java/com/hisabnikash/app/navigation/{Route.kt,HisabNavHost.kt}`
- Create: `app/src/main/java/com/hisabnikash/app/ui/theme/{Color.kt,Theme.kt,Type.kt}`
- Create: `app/src/main/res/values/{strings.xml,colors.xml,themes.xml}` and `app/src/main/res/values-bn/strings.xml`
- Test: `app/src/test/java/com/hisabnikash/app/navigation/RouteTest.kt`

**Interfaces:**
- Produces: `AppContainer`, `Route` sealed interface, `HisabTheme(content)`, and an installable debug app with Dashboard/Transactions/Reports/Savings/Settings placeholders.

- [ ] **Step 1: Write the failing shell test** asserting every bottom-level `Route` has a unique stable path and Bangla/English string resource key.
- [ ] **Step 2: Run `./gradlew testDebugUnitTest --tests '*RouteTest'`** and verify it fails because the project/routes do not exist.
- [ ] **Step 3: Scaffold the Gradle project and minimal Compose navigation shell** with the exact SDK/toolchain constraints above, Material 3 theme, and five bottom destinations.
- [ ] **Step 4: Run `./gradlew testDebugUnitTest assembleDebug lintDebug`** and require all three tasks to succeed.
- [ ] **Step 5: Commit** with `git commit -m "build: scaffold offline finance Android app"`.

### Task 2: Domain model, money parsing, Room schema, and categories

**Files:**
- Create: `core/model/{Transaction.kt,Category.kt,Budget.kt,SavingsGoal.kt,GoalAllocation.kt,RecurringItem.kt,Enums.kt}`
- Create: `core/money/{MoneyParser.kt,MoneyFormatter.kt}`
- Create: `data/db/{HisabDatabase.kt,DbConverters.kt}` and `data/db/entity/*Entity.kt`, `data/db/dao/*Dao.kt`
- Create: `app/schemas/` exported Room schemas
- Create: `data/repository/{TransactionRepository.kt,CategoryRepository.kt,RoomTransactionRepository.kt,RoomCategoryRepository.kt}`
- Test: `core/money/MoneyParserTest.kt`, `data/repository/TransactionRepositoryTest.kt`, `data/repository/CategoryRepositoryTest.kt`
- Test: `androidTest/.../data/repository/RoomRepositoryInstrumentedTest.kt`, `androidTest/.../data/db/MigrationTest.kt`

**Interfaces:**
- Produces: `MoneyParser.parseToMinorUnits(input: String): MoneyParseResult`; `TransactionRepository.observeAll(): Flow<List<Transaction>>`; `add`, `update`, `delete`, and `findPossibleDuplicate`; `CategoryRepository.observeActive(type)` and `archive(id)`.
- Consumes: `AppContainer` from Task 1.

- [ ] **Step 1: Write failing money/category tests** for English and Bangla digits, decimal precision, zero/negative/overflow rejection, seeded categories, custom-category persistence, and archived-category historical joins.
- [ ] **Step 2: Run the three Task 2 test classes** and verify failure from missing models/repositories.
- [ ] **Step 3: Implement immutable domain models and `MoneyParser`/`MoneyFormatter`** using `Long` poisha and locale-aware display.
- [ ] **Step 4: Implement Room entities, DAOs, converters, database callback seeds, and repository mappings**; all writes that affect multiple tables use `withTransaction`.
- [ ] **Step 5: Run `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hisabnikash.app.data.repository.RoomRepositoryInstrumentedTest`**; require green results, then commit `feat: add local finance ledger`.

### Task 3: Quick Entry and transaction history

**Files:**
- Create: `feature/transactions/{QuickEntryViewModel.kt,QuickEntryState.kt,QuickEntryScreen.kt,TransactionListViewModel.kt,TransactionListScreen.kt,TransactionEditor.kt}`
- Create: `ui/components/{AmountField.kt,PaymentMethodDropdown.kt,CategoryDropdown.kt,EmptyState.kt}`
- Modify: `navigation/HisabNavHost.kt`, `AppContainer.kt`
- Test: `feature/transactions/{QuickEntryViewModelTest.kt,TransactionListViewModelTest.kt}`
- Test: `androidTest/.../feature/transactions/QuickEntryScreenTest.kt`

**Interfaces:**
- Consumes: Task 2 repositories and `MoneyParser`.
- Produces: `QuickEntryViewModel.submit(intentionalDuplicate: Boolean = false): SubmitResult`; reusable payment/category dropdowns; edit/delete transaction navigation.

- [ ] **Step 1: Write failing state tests** for defaults, validation, Others one-off labels, saved custom categories, last-used payment method, edit/delete, and duplicate detection with intentional override.
- [ ] **Step 2: Run Task 3 unit tests** and verify they fail on missing ViewModels.
- [ ] **Step 3: Implement Quick Entry and transaction-history state machines** so form input survives retryable save errors and successful saves reset only after repository confirmation.
- [ ] **Step 4: Build the Compose screens** matching the approved mockup: prominent amount, expense/income toggle, description, category, payment dropdown with Cash/Bank/bKash/Nagad treatments, optional details, and one primary save action.
- [ ] **Step 5: Run unit tests, Compose UI tests, `lintDebug`, and `assembleDebug`**; commit `feat: add quick transaction entry`.

### Task 4: Finance calculations, Dashboard, and reports

**Files:**
- Create: `core/time/MonthKey.kt`
- Create: `domain/{FinanceCalculator.kt,DashboardSummary.kt,MonthlyReport.kt}`
- Create: `feature/dashboard/{DashboardViewModel.kt,DashboardState.kt,DashboardScreen.kt}`
- Create: `feature/reports/{ReportsViewModel.kt,ReportsState.kt,ReportsScreen.kt}`
- Create: `ui/components/{SummaryCard.kt,BudgetProgress.kt,TransactionRow.kt}`
- Test: `core/time/MonthKeyTest.kt`, `domain/FinanceCalculatorTest.kt`, `feature/dashboard/DashboardViewModelTest.kt`
- Test: `domain/FinanceCalculatorPerformanceTest.kt`

**Interfaces:**
- Produces: `FinanceCalculator.dashboard(transactions, budgets, allocations, month): DashboardSummary`; `monthlyReport(...): MonthlyReport`; `MonthKey.fromInstant(instant, zone = Asia/Dhaka)`.
- Consumes: Task 2 repositories and Task 3 transaction mutations.

- [ ] **Step 1: Write failing calculation tests** for income, expense, deficit, previous-month comparison, empty months, category/payment breakdown, large ledgers, and instants around Dhaka month boundaries.
- [ ] **Step 2: Run Task 4 tests** and verify missing calculator/time types cause failure.
- [ ] **Step 3: Implement pure calculation and month-boundary services** without Android dependencies.
- [ ] **Step 4: Implement reactive Dashboard and Reports ViewModels/screens** so repository flows recalculate immediately and optional cards collapse to setup prompts.
- [ ] **Step 5: Run Task 4 tests plus a 10,000-transaction benchmark-style JVM test** with a 250 ms calculation budget on the development machine; commit `feat: add dashboard and monthly reports`.

### Task 5: Optional budgets and savings goals

**Files:**
- Create: `data/repository/{BudgetRepository.kt,RoomBudgetRepository.kt,SavingsRepository.kt,RoomSavingsRepository.kt}`
- Create: `feature/budgets/{BudgetViewModel.kt,BudgetScreen.kt}`
- Create: `feature/savings/{SavingsViewModel.kt,SavingsScreen.kt,GoalEditor.kt}`
- Modify: `domain/FinanceCalculator.kt`, `navigation/HisabNavHost.kt`, `AppContainer.kt`
- Test: `feature/budgets/BudgetViewModelTest.kt`, `feature/savings/SavingsViewModelTest.kt`, `domain/FinanceCalculatorSavingsTest.kt`

**Interfaces:**
- Produces: monthly total/category budget CRUD; savings goal CRUD; `allocate(goalId, amountMinor)` and `reverse(allocationId)`; allocation never creates an expense transaction.
- Consumes: Task 4 summaries and Task 2 database entities.

- [ ] **Step 1: Write failing tests** for no-budget use, total/category remaining values, exceeded state, negative monthly savings, goal allocation/reversal, and allocation totals exceeding available savings.
- [ ] **Step 2: Run Task 5 tests** and confirm failure on missing repositories.
- [ ] **Step 3: Implement budget and savings repositories plus validation rules** with atomic allocation writes.
- [ ] **Step 4: Implement optional Budget and Savings Compose flows** with setup prompts, goal progress, and clear deficit messaging.
- [ ] **Step 5: Run all unit/UI tests and commit** `feat: add optional budgets and savings goals`.

### Task 6: Recurring reminders and confirmation actions

**Files:**
- Create: `data/repository/{RecurringRepository.kt,RoomRecurringRepository.kt}`
- Create: `feature/recurring/{RecurringViewModel.kt,RecurringScreen.kt,RecurringEditor.kt,RecurringScheduler.kt,RecurringWorker.kt,ReminderReceiver.kt,NotificationFactory.kt}`
- Modify: `AndroidManifest.xml`, `HisabApplication.kt`, `AppContainer.kt`
- Test: `feature/recurring/{RecurringSchedulerTest.kt,RecurringWorkerTest.kt,RecurringViewModelTest.kt}`

**Interfaces:**
- Produces: recurring template CRUD; `schedule(item)`, `cancel(id)`, `confirmOccurrence(id, edits?)`, `skipOccurrence(id)`; notification actions never write a transaction without confirmation.
- Consumes: `TransactionRepository.add` and WorkManager.

- [ ] **Step 1: Write failing tests** for due-date advancement, reboot/reschedule, quick confirm, open-and-review, edit, skip, disable, duplicate WorkManager jobs, and denied notification permission.
- [ ] **Step 2: Run Task 6 tests** and verify failure.
- [ ] **Step 3: Implement repository, unique WorkManager scheduling, and notification actions** using stable recurring IDs.
- [ ] **Step 4: Implement recurring editor/list and permission education UI**; dismissed notifications leave totals unchanged.
- [ ] **Step 5: Run unit/worker tests and manually trigger one worker with `adb`**; commit `feat: add recurring transaction reminders`.

### Task 7: Settings, DataStore preferences, and complete localization

**Files:**
- Create: `data/preferences/UserPreferences.kt`
- Create: `feature/settings/{SettingsViewModel.kt,SettingsScreen.kt,LanguageController.kt,DataDeletionUseCase.kt}`
- Modify: all `strings.xml` files and all screens containing provisional copy
- Test: `feature/settings/{UserPreferencesTest.kt,DataDeletionUseCaseTest.kt}`
- Test: `androidTest/.../LocalizationSmokeTest.kt`

**Interfaces:**
- Produces: persisted language, last payment method, guest-warning dismissal, backup toggles/URI, and `deleteAllLocalData()`.
- Consumes: every repository's clear operation and Task 1 navigation.

- [ ] **Step 1: Write failing preference/deletion/localization tests** covering process recreation, Bangla resource completeness, and deletion confirmation.
- [ ] **Step 2: Run Task 7 tests** and verify failure.
- [ ] **Step 3: Implement Preferences DataStore and locale switching** without restarting into a partially translated UI.
- [ ] **Step 4: Complete Settings, guest backup warnings, and destructive data deletion flow**; remove all hard-coded user-visible strings.
- [ ] **Step 5: Run unit tests, localization smoke test, and `lintDebug`**; commit `feat: add localized settings and privacy controls`.

### Task 8: Versioned snapshot and authenticated encryption

**Files:**
- Create: `backup/model/{BackupEnvelope.kt,BackupSnapshotV1.kt}`
- Create: `backup/crypto/{BackupCrypto.kt,AndroidBackupSecretStore.kt}`
- Create: `backup/{SnapshotExporter.kt,SnapshotImporter.kt,RestoreValidator.kt}`
- Test: `backup/{BackupCryptoTest.kt,SnapshotRoundTripTest.kt,RestoreValidatorTest.kt}`

**Interfaces:**
- Produces: `BackupCrypto.encrypt(snapshotBytes, password): BackupEnvelope`; `decrypt(envelope, password): ByteArray`; `SnapshotExporter.export(): BackupSnapshotV1`; `SnapshotImporter.validate(bytes, password): ValidatedRestore`; `apply(validated): RestoreResult`.
- Consumes: all Room repositories through a database transaction boundary.

- [ ] **Step 1: Write failing crypto/round-trip tests** for random salt/nonce, identical plaintext producing different ciphertext, correct restore, wrong password, tampering, unsupported version, missing tables, and database unchanged after any validation failure.
- [ ] **Step 2: Run Task 8 tests** and verify failure.
- [ ] **Step 3: Implement PBKDF2-HMAC-SHA256 key derivation, AES-256-GCM envelope, versioned serialization, and Android Keystore-protected local password cache**; zero sensitive byte arrays where practical.
- [ ] **Step 4: Implement export, full validation, rollback snapshot, and atomic restore** with no mutation before `ValidatedRestore` exists.
- [ ] **Step 5: Run Task 8 tests plus static log scan** for sensitive fields; commit `feat: add encrypted backup format`.

### Task 9: Persistent on-device backup and reinstall restore

**Files:**
- Create: `backup/local/{LocalBackupStore.kt,LocalBackupWorker.kt,DocumentTreeAccess.kt}`
- Create: `backup/BackupScheduler.kt`
- Modify: `data/repository/RoomTransactionRepository.kt`, `data/repository/RoomCategoryRepository.kt`, `data/repository/RoomBudgetRepository.kt`, `data/repository/RoomSavingsRepository.kt`, `data/repository/RoomRecurringRepository.kt`
- Modify: `MainActivity.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `AndroidManifest.xml`
- Test: `backup/local/{LocalBackupStoreTest.kt,LocalBackupWorkerTest.kt}`
- Test: `androidTest/.../backup/LocalRestoreInstrumentedTest.kt`

**Interfaces:**
- Produces: `BackupScheduler.onDataChanged()`, `chooseBackupLocation()`, `backUpNow(password)`, queued unique backup after data changes, and `restoreFromDocument(uri, password)`.
- Consumes: Task 8 exporter/importer and Storage Access Framework URIs.

- [ ] **Step 1: Write failing tests** for persisted URI access, atomic temporary-file replacement, unavailable/removable storage, queued-write coalescing, manual backup, reinstall-style import, and current-data preservation on failure.
- [ ] **Step 2: Run Task 9 tests** and verify failure.
- [ ] **Step 3: Implement SAF activity-result bridges, local store, and coalesced WorkManager writes** without broad storage permission.
- [ ] **Step 4: Implement Settings backup/restore UX** with password prompts, last-success timestamp, risk explanation, and actionable errors.
- [ ] **Step 5: Run automated tests and a manual uninstall/reinstall restore on an emulator or physical device**; commit `feat: add persistent phone backup and restore`.

### Task 10: User-owned Google Drive backup and restore

**Files:**
- Create: `backup/drive/{DriveAuthorizationClient.kt,DriveRestClient.kt,DriveBackupStore.kt,DriveBackupWorker.kt}`
- Modify: `SettingsViewModel.kt`, `SettingsScreen.kt`, `AppContainer.kt`, `app/build.gradle.kts`
- Test: `backup/drive/{DriveRestClientTest.kt,DriveBackupStoreTest.kt,DriveBackupWorkerTest.kt}`

**Interfaces:**
- Produces: `authorizeDriveAppData(activity): DriveAuthorizationResult`; `uploadLatest(envelope)`; `downloadLatest()`; `revoke()`; background retry status exposed as `Flow<BackupStatus>`.
- Consumes: Task 8 encrypted envelopes and Google Authorization `drive.appdata` access token.

- [ ] **Step 1: Write failing HTTP contract tests** for appDataFolder search, create/update multipart upload, newest valid download, 401 reauthorization, offline retry, quota/server errors, and never sending decrypted bytes.
- [ ] **Step 2: Run Task 10 tests** against a fake HTTP transport and verify failure.
- [ ] **Step 3: Implement Google AuthorizationClient integration and minimal Drive v3 REST client**; request only the app-data scope and keep access tokens out of persistence/logs.
- [ ] **Step 4: Implement opt-in Drive backup/restore UI and constrained WorkManager retry**; local ledger remains usable for every remote failure.
- [ ] **Step 5: Run fake-client tests and a manual OAuth/backup/clean-install restore with a test Google account**; commit `feat: add user-owned Drive backup`.

### Task 11: Release hardening and closed-distribution artifact

**Files:**
- Create: `docs/testing/manual-release-checklist.md`, `docs/privacy/data-handling.md`, `README.md`
- Modify: `app/proguard-rules.pro`, `AndroidManifest.xml`, Gradle build files, all tests as failures reveal gaps
- Test: entire `app/src/test` and `app/src/androidTest` suites

**Interfaces:**
- Consumes: complete app from Tasks 1–10.
- Produces: reproducible signed-or-debug closed-test APK, test report, privacy/data-flow documentation, and no known critical restore/calculation defects.

- [ ] **Step 1: Add an end-to-end fixture test** that creates several months of income/expenses, budgets, a custom category, goal allocation, and recurring item; export/restore it and assert identical Dashboard/Reports.
- [ ] **Step 2: Run `./gradlew clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`** and record exact results; fix failures at their owning layer rather than weakening assertions.
- [ ] **Step 3: Run manual checks** for airplane mode, process death, reboot reminder, Bangla/English, 10,000 transactions, wrong-password restore, local reinstall restore, and Drive clean-install restore.
- [ ] **Step 4: Review release security**: manifest permissions, exported components, logs, backup rules, cleartext traffic, ProGuard/R8, OAuth scope, and absence of embedded secrets; document results.
- [ ] **Step 5: Build the closed-test APK, record SHA-256, update README install instructions, and commit** `release: prepare closed personal finance test build`.

## Completion Evidence

Implementation is complete only when:

- every task commit exists in order;
- `clean`, unit tests, lint, debug assembly, and available connected tests pass in one final run;
- any test requiring unavailable external credentials or a physical device is explicitly marked unverified rather than claimed as passing;
- the APK path and SHA-256 are reported;
- the final implementation is checked against every section of the approved spec.
