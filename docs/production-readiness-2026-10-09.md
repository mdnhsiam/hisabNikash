# HisabNikash release readiness

Assessment started 2026-10-09; candidate verification 2026-10-10 (Asia/Dhaka). Scope: the approved free, Bangla/English, offline-first Android app for closed use. No public hosting, developer-operated server, analytics or paid tier has been added.

## Status

HisabNikash 0.1.1 is a locally built, debug-signed test candidate. The clean automated gates passed at application source `14bdfb8e911a0422cd48f72a5bea7f37e2433674`; independent review returned four Important findings, all repaired with failing-then-passing regressions and the green whole suite. This document is not production sign-off. Real-device restore, Google authorization and owner signing remain unverified. No claim of zero defects, complete penetration testing or OWASP certification is made.

## Repaired areas

- Financial repository writes and authenticated restores validate amount bounds, category/type relationships, budget month keys and allocation/reversal integrity. Invalid imports leave the existing tables unchanged.
- Real SQLite tests cover Room migrations v1→v3/v2→v3, SQL metacharacters stored as values, missing-row updates and all-table restore rollback.
- Totals use checked arithmetic; budget percentages cannot silently overflow. Updating a savings goal preserves contribution history.
- Recurring confirmation/skip use the displayed occurrence token. Stale or disabled actions fail safely. Reminder notification identities do not depend on hash uniqueness, and lock-screen public content omits financial details.
- Current-month views refresh on resume and while visible, retaining their shared clock across Activity/composition recreation. Monthly savings is separate from lifetime funds available for goals.
- Failed transaction edits and category renames retain their draft; category selection must match income/expense. Long forms scroll, repeated submissions are disabled, dates accept Bengali digits, and errors are localized.
- Local deletion cancels recurring work and clears posted notifications; reminder lookup/posting shares the deletion boundary. Folder selection or Drive connection remains visibly incomplete until the first encrypted backup succeeds.
- Reports show daily/monthly income and expenses. Optional category budgets show progress on the dashboard. Navigation uses distinct native glyphs.

Backup/restore/disable/deletion now share a serialized operation gate. Recovery capture joins the Room replacement transaction; failed file promotion retains a recoverable previous copy. Manual Drive actions refresh authorization, stale consent cannot reconnect after disconnect/deletion, and cancellation wipes password arrays. New snapshots use 600,000-iteration SHA256 derivation with a format-compatible minimum-OS fallback; older backups remain readable. Platform extraction rules exclude all nine data domains. Settings explains separate owner-controlled deletion of external backup copies. Actual Android provider, OAuth and storage behavior still require device verification.

## Verification record

| Check | Evidence/status |
| --- | --- |
| First persistence checkpoint | 98 JVM tests, zero failures; commit `bad1fae` |
| Final complete JVM/Compose/SQLite suite | Clean run: 44 suites, 151 tests, zero failures/errors/skips |
| Debug and release lint | Both passed; zero lint issues |
| Debug and minified unsigned release builds | Both passed; 0.1.1/code 2, minimum API24/target API36; release non-debuggable, debug signature verifies, both pass 16K zip alignment |
| Android instrumentation compilation | Passed in final clean run; both test APKs also assembled |
| Android instrumentation execution | Gate attempted: `No connected devices!`; software emulator stayed offline and diagnostic restart exited before boot. Not passing |
| Runtime dependency advisories | Final resolved inventory unchanged:107 packages; refreshed OSV check returned no known affected packages. This is not proof of no vulnerabilities |
| Hosted CI | YAML/action/Bash/scanner checks passed; real GitHub jobs not run |
| Independent final review | One fresh whole-branch review; four Important findings repaired RED→GREEN, final suite151/151. [Review record](testing/production-hardening-review.md) |

The manual CI workflow has no automatic triggers. It requires owner confirmation of available included minutes and a no-overage spending limit, uses pinned official actions/read-only permissions, and has bounded jobs. No billing setting was changed. [Security and dependency assessment](production-security-assessment-2026-10-09.md)

## Owner/device gates before production

1. Test on a physical Android phone: airplane-mode entry/edit/delete, restart, reboot, denied permissions, reminders, large ledger, Bangla/English, large text and TalkBack.
2. Enable a phone-folder backup, verify the file outside app storage, uninstall/reinstall, select the backup again and restore it. Test wrong password, insufficient storage and the actual storage provider's rename behavior. Do not uninstall a phone containing unbacked-up real data.
3. Configure Google's Android OAuth client for `com.hisabnikash.app` and the actual signing certificate, enable Drive access/consent for approved test users, then test real sign-in, denial, expiry, disconnect/reconnect and clean-install restore. Use only the user's own Drive. No credentials should be committed.
4. Generate and securely back up an owner-controlled release signing key outside the repository. Sign the minified release, verify its certificate/checksum, and test clean installation and update over the previous signed version. Debug signing is not production signing.
5. Execute the API 24 and API 36 instrumentation gates only after verifying free Actions allowance and a no-paid-overage spending policy. Re-run the dependency advisory gate for the signed release's selected dependencies.

Until these gates have recorded results, the APK is a test candidate, not a guaranteed production release. A native Android APK does not require web hosting; optional Google Drive backup requires the owner's Google configuration.

## Security boundary

The local Room database relies on Android app isolation, device encryption and device lock; it is not separately encrypted by the app. Explicit backups use authenticated encryption, and opted-in background-backup secrets use Android Keystore protection. The app does not protect against a rooted/compromised or already-unlocked device. No concurrent live multi-device synchronization is promised.

The acceptance model follows [Android core app quality](https://developer.android.com/docs/quality-guidelines/core-app-quality) and uses [OWASP MASVS](https://mas.owasp.org/MASVS/) as a review checklist, not a certification. Passing automated checks does not replace real-device restore and authorization testing.
