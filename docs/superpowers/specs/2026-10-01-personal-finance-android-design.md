# Personal Finance Android App — Design Specification

**Date:** 2026-10-01  
**Status:** Ready for user review  
**Initial distribution:** Free, closed Android distribution for up to 20 trusted devices

## 1. Product intent

Build a lightweight, dependable personal-finance Android app for users in Bangladesh. A user records income or expenses once; the app automatically updates the dashboard, budgets, savings, category totals, recurring reminders, and monthly reports.

The product prioritizes:

- minimal effort and one- or two-tap access to frequent actions;
- reliable offline use;
- clear Bangla and English interfaces;
- a modern, restrained, premium-looking visual style;
- privacy and user ownership of financial data;
- no recurring infrastructure or hosting cost in the initial release;
- a focused feature set that remains easy to understand and test.

Receipt scanning, OCR, a central developer-owned database, live concurrent multi-device editing, and paid tiers are outside the initial scope.

## 2. Audience and localization

The initial audience is an individual in Bangladesh managing personal income, expenses, budgets, and savings.

- Primary currency: BDT, displayed with `৳`.
- Languages: Bangla and English, selectable in settings.
- Payment methods: Cash, Bank, bKash, and Nagad.
- Production UI should use permitted official bKash and Nagad brand assets. Neutral icons are used for Cash and Bank.
- Dates, numbers, and month labels follow the selected language and Bangladesh locale conventions.

## 3. Platform and distribution

- Native Android application.
- Offline-first operation; network access is not required for core features.
- Initial release uses Google's no-fee limited distribution or direct trusted-device installation and targets no more than 20 devices.
- Public Play Store distribution and its registration cost are deferred until the product is validated.
- The complete initial feature set is free. There is no free/premium split.

## 4. User experience principles

### 4.1 Progressive disclosure

The default flow shows only essential controls. Notes, custom categories, category budgets, savings goals, recurring settings, local backup, and cloud backup are optional.

### 4.2 Smart defaults

- New transactions default to today's date.
- The most recently used payment method is preselected.
- The app may suggest a previously used category for matching descriptions, using local history only.
- A user can complete a normal entry without configuring accounts, budgets, goals, or backup.

### 4.3 No duplicate input

A saved transaction is the single source for the dashboard, reports, budgets, savings, and recurring-history views. Users do not re-enter the same value elsewhere.

## 5. Navigation and core screens

The main navigation contains:

1. **Dashboard** — current month overview and recent activity.
2. **Transactions** — searchable and filterable transaction history.
3. **Reports** — monthly and category summaries.
4. **Savings** — calculated savings and optional goals.
5. **Settings** — language, categories, recurring items, backup, restore, export, and data deletion.

A persistent primary `+` action opens Quick Entry from the Dashboard and other primary screens.

## 6. Dashboard

The Dashboard shows the current month by default:

- total income;
- total expenses;
- calculated unspent amount (`income - expenses`);
- optional total monthly budget and remaining budget;
- automatic monthly savings;
- optional savings-goal progress;
- comparison with the previous month;
- recent transactions;
- compact category summary.

If the user has not configured a budget or goal, the related card is replaced by a lightweight setup prompt rather than an empty complex widget.

## 7. Transactions and Quick Entry

### 7.1 Required fields

- Type: Income or Expense.
- Amount: positive BDT value.
- Description: where the money came from or what it was used for.

### 7.2 Defaulted or optional fields

- Date/time: defaults to now and can be changed.
- Category: selected from the applicable list; a local-history suggestion may be shown.
- Payment method: Cash, Bank, bKash, or Nagad; defaults to the last-used method.
- Note: optional.

### 7.3 Categories

The app ships with a small practical set of income and expense categories. Expense examples include Food, Transport, Groceries, Bills, Medical, Shopping, Education, Entertainment, and Others.

When a user selects **Others**, they can enter a custom label and choose one of two explicit actions:

- **Use only for this transaction** — the default; it does not expand the category list.
- **Save as a category for future use** — creates a reusable custom category.

Users can rename, reorder, or archive custom categories. Archiving never removes historical transactions.

### 7.4 Transaction integrity

- Amount must be greater than zero.
- Invalid required fields block saving and display an inline message.
- The database write is atomic. The UI updates only after a successful write.
- Rapidly repeated equivalent submissions trigger a duplicate warning rather than silently creating a second entry.
- Users can edit or delete a transaction. Deletion requires confirmation and recalculates dependent views.

## 8. Budgets

Budgets are optional.

- A user may set one total monthly budget.
- A user may additionally set budgets for selected expense categories.
- No budget is inferred from income without user action.
- The Dashboard shows spent amount, remaining amount, percentage used, and a clear exceeded state.
- Budget configuration is not required to use transaction tracking or reports.

## 9. Savings

### 9.1 Automatic monthly savings

The app calculates monthly savings as:

`total income - total expenses`

This value can be negative and must be presented honestly as a monthly deficit.

### 9.2 Optional savings goals

Users may create named goals such as Emergency Fund or New Phone, set an optional target amount, and allocate part of their calculated or existing savings to a goal.

An allocation is an earmarking operation, not an expense. It does not reduce total net savings. Goal contributions and reversals are recorded so progress is auditable.

## 10. Recurring items and notifications

Users can create recurring income or expense templates with an amount, description, category, payment method, frequency, and next due date.

Two reminder styles are available:

- **Open and review** — the notification opens the populated transaction form for review and saving.
- **Quick confirm** — the notification offers a one-tap confirmation while retaining edit and skip actions.

Both styles require user confirmation before committing the transaction in the initial release. Nothing is silently added to the ledger.

From the notification or recurring screen, the user can:

- confirm unchanged;
- edit and confirm;
- skip this occurrence;
- disable the recurring item.

Skipping or dismissing a notification does not affect financial totals. Notification permission denial is explained, and recurring items remain visible inside the app.

## 11. Reports

The initial reports include:

- daily and monthly income and expense totals;
- category breakdown;
- payment-method breakdown;
- monthly savings or deficit;
- comparison with the previous month;
- optional budget performance;
- optional savings-goal progress.

Reports use the local transaction database and remain available offline. Advanced forecasting and AI recommendations are excluded.

## 12. Data architecture

### 12.1 Local source of truth

The Android device's local database is the source of truth. A transaction is written locally before any backup operation. Dashboard and report calculations observe local data and update immediately.

Core local units are:

- transaction repository;
- category repository;
- budget repository;
- savings-goal repository;
- recurring-item repository;
- settings repository;
- reporting/calculation services;
- backup and restore service;
- reminder scheduler.

Each unit exposes a focused interface so calculations, persistence, UI, and backup can be tested independently.

### 12.2 Initial device-sync boundary

The first release supports backup and restore, not simultaneous live editing on multiple devices. A second device can restore the latest backup and later publish a new backup. Concurrent record-level synchronization and conflict resolution are deferred because they require substantially more complexity or a server.

The UI must describe this as **Backup and restore**, not imply real-time live sync.

## 13. Backup and restore

Backup is optional. The app works fully without an account.

### 13.1 User-owned Google Drive

- The user signs in with Google and explicitly enables Drive backup.
- The app requests the narrow app-data scope.
- Encrypted backup snapshots are stored in the signed-in user's private Google Drive app-data folder.
- No financial data is sent to a developer-owned Drive, database, analytics service, or server.
- A failed upload never blocks or rolls back a local transaction.
- Pending backup retries when network access returns.
- A new installation can sign in to the same Google account and restore the latest valid snapshot.

### 13.2 On-device persistent backup file

- The user may enable **Keep a backup on this phone**.
- Android's Storage Access Framework opens the system picker so the user chooses a shared-storage file or folder.
- The backup file is stored outside app-specific storage and remains after uninstall.
- When enabled, a queued background write refreshes the backup after each successful data-changing operation; **Back up now** is also available.
- After reinstall, **Restore from phone** lets the user select that file.
- No broad storage permission is requested.
- The app explains that device reset, storage failure, or manual file deletion will destroy this backup.

### 13.3 Encryption and recovery

- Backup contents are encrypted before being written to Drive or shared device storage.
- Enabling encrypted backup requires a backup password.
- The password is not uploaded with the backup.
- The current device stores the derived protection material using Android secure storage where supported.
- Reinstalling or restoring on another device requires the backup password.
- The app clearly warns that a forgotten backup password cannot be recovered in this no-server architecture.

### 13.4 Safe restore

Before replacing local data, restore verifies:

- supported backup format and schema version;
- integrity/authentication of encrypted content;
- correct backup password;
- successful parse and database validation.

The current database remains untouched until verification succeeds. A rollback copy is created immediately before applying a valid restore.

## 14. Guest mode and warnings

Guest mode is the default viable path and does not require Google sign-in.

Users without either Drive backup or persistent on-device backup see a concise warning during onboarding and periodically in Settings: uninstalling the app, losing the phone, or clearing app data can permanently remove their financial data.

The warning must be informative, not coercive. Core functionality remains accessible after dismissal.

## 15. Error handling

- **Database write failure:** keep the form values, do not update totals, and show a retryable message.
- **Insufficient storage:** explain the condition and keep unsaved input in memory while the screen remains open.
- **Drive offline or upload failure:** retain local data, mark backup pending, and retry later.
- **Google authorization expiry:** request sign-in again without affecting local records.
- **Wrong backup password or corrupt backup:** reject restore without modifying the database.
- **Unsupported backup version:** explain that a compatible app version is required.
- **Notification permission denied:** preserve recurring schedules in-app and show how to enable notifications.
- **Duplicate transaction:** warn with the suspected match and let the user cancel or save intentionally.

All messages must be actionable, localized, and avoid exposing secrets or raw stack traces.

## 16. Security and privacy

- Financial data is local by default.
- Cloud backup is opt-in and user-owned.
- Backup files are encrypted before leaving app-private memory.
- Secrets and tokens use Android secure storage; no developer credentials or private refresh tokens are embedded in the app.
- Logs must not contain transaction descriptions, amounts, tokens, passwords, or decrypted backup content.
- Users can delete all local data and can remove their backup through an explicit settings flow.
- The app collects no analytics in the initial release.

## 17. Testing and quality gates

### 17.1 Automated tests

- Unit tests for totals, monthly grouping, budgets, category breakdown, savings, deficits, and goal allocations.
- Database tests for create, edit, delete, atomic writes, custom categories, recurring templates, and schema migrations.
- Backup round-trip tests: serialize, encrypt, decrypt, validate, restore, and compare records.
- Failure tests for wrong password, corrupted file, unsupported version, storage failure, network loss, and expired Google authorization.
- UI tests for Quick Entry, payment/category dropdowns, optional fields, Bangla/English, and immediate Dashboard refresh.
- Reminder tests for confirm, edit, skip, disable, and notification-permission denial.

### 17.2 Manual release checks

- Run on at least one physical Android phone.
- Verify core use with airplane mode enabled.
- Verify process restart and device reboot retain data and scheduled reminders.
- Verify persistent local backup survives uninstall and restores after reinstall.
- Verify Google Drive backup and restore on a clean installation.
- Verify both supported languages and BDT formatting.
- Verify usable performance with several years and thousands of transactions.

No release is considered complete while required automated tests fail or a critical manual restore path is unverified.

## 18. Initial implementation boundaries

Included:

- native Android UI and navigation;
- local transactions, categories, budgets, goals, and recurring items;
- offline Dashboard and reports;
- notifications and confirmation flow;
- encrypted local backup and restore;
- optional user-owned Google Drive backup and restore;
- Bangla and English localization;
- testing described above.

Deferred:

- receipt scanning or OCR;
- bank, bKash, or Nagad transaction ingestion;
- developer-owned cloud storage;
- live concurrent multi-device synchronization;
- public Play Store launch;
- subscriptions, advertising, or premium tiers;
- predictive analytics or AI financial advice.

## 19. Success criteria

The initial release succeeds when a user can:

1. install and use the app without an account or internet connection;
2. add a normal income or expense in a short, understandable flow;
3. see accurate Dashboard and report updates immediately;
4. optionally configure budgets, custom categories, recurring items, and savings goals;
5. receive and confirm recurring reminders;
6. create an encrypted backup in their own Drive or a persistent phone folder;
7. restore a verified backup after reinstall without corrupting current data;
8. use the app comfortably in Bangla or English;
9. complete all of the above without any developer-operated server or recurring infrastructure cost.
