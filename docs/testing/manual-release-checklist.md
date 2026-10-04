# Hisab 0.1.0 closed-test checklist

## Automated evidence

- [x] Multi-month income/expense, custom category, budget, savings allocation and recurring-item encrypted round trip preserves Dashboard and Reports.
- [x] Wrong-password and corrupt-backup restore leave current data unchanged.
- [x] Bangla resource coverage matches English resources.
- [x] 10,000-transaction report fixture meets the JVM performance threshold.
- [x] Offline/retry/quota/auth Drive behavior is covered by fake HTTP contract tests.
- [x] Unit tests, Android instrumentation compilation, lint and debug APK assembly pass.

## Real-device checks required before wider distribution

- [ ] Airplane mode: add/edit/delete transactions and confirm dashboard/report updates.
- [ ] Process death: enter data, force-stop, reopen and confirm persistence.
- [ ] Reboot: confirm enabled recurring reminders are rescheduled and notification opens the review flow.
- [ ] Switch Bangla/English and inspect every screen for clipping and missing strings.
- [ ] Import/scroll/search a 10,000-transaction ledger on the minimum supported device.
- [ ] Select a phone folder, uninstall/reinstall, then restore the encrypted backup.
- [ ] With a registered Google OAuth client and test account, back up, clear/install clean, reconnect and restore from Drive.
- [ ] Deny notification permission and verify ledger use remains unaffected.

The unchecked cases are explicitly unverified because this workspace has no connected physical device, hardware-accelerated emulator, or registered Google OAuth client. They must not be treated as passing.
