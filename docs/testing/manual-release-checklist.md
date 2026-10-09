# HisabNikash 0.1.1 closed-test checklist

## Automated evidence

- [x] Multi-month income/expense, custom category, budget, savings allocation and recurring-item encrypted round trip preserves Dashboard and Reports.
- [x] Wrong-password and corrupt-backup restore leave current data unchanged.
- [x] Bangla resource coverage matches English resources.
- [x] 10,000-transaction report fixture meets the JVM performance threshold.
- [x] Offline/retry/quota/auth Drive behavior is covered by fake HTTP contract tests.
- [x] Unit tests, Android instrumentation compilation, lint and debug APK assembly pass.
- [x] Final-review regressions: retained month clock after composition recreation, failed category-rename draft retention, reminder cancellation/deletion boundary, and truthful first-backup readiness.

## Real-device checks required before wider distribution

- [ ] Airplane mode: add/edit/delete transactions and confirm dashboard/report updates.
- [ ] Process death: enter data, force-stop, reopen and confirm persistence.
- [ ] Reboot: confirm enabled recurring reminders are rescheduled and notification opens the review flow.
- [ ] Switch Bangla/English and inspect every screen for clipping and missing strings.
- [ ] Import/scroll/search a 10,000-transaction ledger on the minimum supported device.
- [ ] Select a phone folder, uninstall/reinstall, then restore the encrypted backup.
- [ ] Select/connect without creating a snapshot: verify incomplete setup remains visible; then create the first encrypted backup and confirm status. Switch folders and repeat; keep the password safely outside the app.
- [ ] With a registered Google OAuth client and test account, back up, clear/install clean, reconnect and restore from Drive.
- [ ] Deny notification permission and verify ledger use remains unaffected.
- [ ] Run encrypted backup/restore and wrong-password rejection on API 24/25 and a current physical Android device; measure the new 600,000-iteration derivation time.
- [ ] Confirm stale/repeated reminder actions cannot charge or skip a newer occurrence, and lock-screen public content hides amounts/descriptions.
- [ ] Leave the app open over a Dhaka month boundary; resume it in the new month and verify dashboard, budgets and monthly savings without adding a transaction, also after rotating or changing language.
- [ ] Verify TalkBack labels, 200% text size, small-screen dialogs, portrait/landscape, Bangla numbers, and dark mode on every screen.
- [ ] Exercise denied/revoked folder permission, low storage and failed rename on the chosen real document provider; recover the previous backup.
- [ ] Confirm local data deletion clears background credentials/reminders; folder/Drive copies remain until explicitly deleted by their owner.
- [ ] Test OAuth consent denial, expired/revoked authorization and disconnect/reconnect using registered signing certificates and approved accounts.
- [ ] Sign the minified release with an owner-controlled, securely backed-up key; verify clean installation, updates, certificate and checksum. Do not distribute a debug-signed candidate as production.
- [ ] Verify account free CI allowance and stopped paid overage, then execute both Android instrumentation jobs. A skipped/failed gate is not a pass.

The unchecked cases are explicitly unverified because this workspace has no connected physical device, hardware-accelerated emulator, or registered Google OAuth client. They must not be treated as passing.
