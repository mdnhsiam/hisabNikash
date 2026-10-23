# HisabNikash closed-test checklist

Current published test candidate:0.1.6/code7. [2026-10-23 setup audit and OAuth runbook](setup-status-2026-10-23.md), [177-test verification and published artifact](font-oauth-2026-10-23.md), [production preparation and explicit release hold](production-preparation-2026-10-23.md). Earlier evidence below is dated; it does not substitute for candidate/device checks. Do all destructive/clean-install tests only on a spare/test installation with disposable fixture data, never the owner's unbacked-up ledger.

## Automated evidence

- [x] Multi-month income/expense, custom category, budget, savings allocation and recurring-item encrypted round trip preserves Dashboard and Reports.
- [x] Wrong-password and corrupt-backup restore leave current data unchanged.
- [x] Bangla resource coverage matches English resources.
- [x] 10,000-transaction report fixture meets the JVM performance threshold.
- [x] Offline/retry/quota/auth Drive behavior is covered by fake HTTP contract tests.
- [x] Unit tests, Android instrumentation compilation, lint and debug APK assembly pass.
- [x] Generated dashboard/launcher logo, API24/API26/modern resources, Bangla identity at 200% text and circular-safe-area rendering regressions pass; complete clean suite171/171 and both lint reports contain zero issues.
- [x] Final-review regressions: retained month clock after composition recreation, failed category-rename draft retention, reminder cancellation/deletion boundary, and truthful first-backup readiness.

- [x] Cold initialization in fresh class loaders and real Application/MainActivity modeled API24/35/36 startup, plus API35/36 recreation pass. These are Robolectric checks, not device execution.

- [x] Home hidden/reveal semantics, pause/reset, draft resize/recreation/cancel/discard, date-error recovery and Dhaka month/type drilldown regressions pass.

## Distribution follow-through

- [x] Upload the verified0.1.4 APK to GitHub and verify remote digest. Published private `v0.1.4-alpha.1` on 2026-10-22 (Asia/Dhaka); asset `663443714`, 16,822,054 bytes, SHA256 matches the verified local APK. See the release test report. Older releases remain unchanged.

## Real-device checks required before wider distribution

- [ ] On0.1.6 verify monthly totals are visible from launch without a global eye button. Only recent transactions require local show/hide; background/recreation/navigation re-hide those entries. TalkBack must not announce hidden entries. This is not an app lock. Inspect Hind headings/amounts and Noto prose in both languages, including200% text and long amounts.
- [ ] Test partial/full sheet by drag and labeled controls, actual keyboard insets, large text and landscape. Test cancel/discard, invalid date recovery, failed save, duplicate warning and rotation. Unsaved system-process-death draft recovery is not promised.

- [ ] On Nothing Phone3a/Android16 install the verified candidate over the existing app without uninstall/data clear; verify launch, recreation and existing ledger preservation. Capture the actual crash log if failure persists.

- [ ] Verify launcher appearance under the device's standard/themed icon settings and dashboard branding in Bangla/English with large text and TalkBack.
- [ ] Airplane mode: add/edit/delete transactions and confirm dashboard/report updates.
- [ ] Process death: enter data, force-stop, reopen and confirm persistence.
- [ ] Reboot: confirm enabled recurring reminders are rescheduled and notification opens the review flow.
- [ ] Switch Bangla/English and inspect every screen for clipping and missing strings.
- [ ] Import/scroll/search a 10,000-transaction ledger on the minimum supported device.
- [ ] On a spare/test installation, select a phone folder, verify a completed backup outside private app storage, uninstall/reinstall only that disposable installation, then restore the encrypted backup.
- [ ] Select/connect without creating a snapshot: verify incomplete setup remains visible; then create the first encrypted backup and confirm status. Switch folders and repeat; keep the password safely outside the app.
- [ ] With the configured Android test OAuth client and approved account, back up a disposable fixture, then on a spare/test installation reconnect and restore from Drive. Never clear the owner's real ledger to test this.
- [ ] Deny notification permission and verify ledger use remains unaffected.
- [ ] Run encrypted backup/restore and wrong-password rejection on API 24/25 and a current physical Android device; measure the new 600,000-iteration derivation time.
- [ ] Confirm stale/repeated reminder actions cannot charge or skip a newer occurrence, and lock-screen public content hides amounts/descriptions.
- [ ] Leave the app open over a Dhaka month boundary; resume it in the new month and verify dashboard, budgets and monthly savings without adding a transaction, also after rotating or changing language.
- [ ] Verify TalkBack labels,200% text size, small-screen dialogs, portrait/landscape, Bangla numbers, and legibility under the device's light/dark system preference. The current app explicitly uses its light palette; automatic dark-theme support is not promised.
- [ ] Exercise denied/revoked folder permission, low storage and failed rename on the chosen real document provider; recover the previous backup.
- [ ] Confirm local data deletion clears background credentials/reminders; folder/Drive copies remain until explicitly deleted by their owner.
- [ ] Test OAuth consent denial, expired/revoked authorization and disconnect/reconnect using registered signing certificates and approved accounts.
- [ ] Sign the minified release with an owner-controlled, securely backed-up key; verify clean installation, updates, certificate and checksum. Do not distribute a debug-signed candidate as production.
- [ ] Verify account free CI allowance and stopped paid overage, then execute both Android instrumentation jobs. A skipped/failed gate is not a pass.

The unchecked cases are explicitly unverified because this workspace has no connected physical device or executed hardware-accelerated emulator checks. The Android test OAuth client is configured, but real phone consent and Drive restore have not been exercised. These cases must not be treated as passing. Public OAuth/production release is explicitly on hold until owner approval and completion of the production gates.
