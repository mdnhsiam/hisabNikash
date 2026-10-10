# HisabNikash production security assessment

Assessment started 2026-10-09, core refresh 2026-10-10, branding/runtime refresh 2026-10-11 (Asia/Dhaka). Scope: the private, closed-distribution Android application, its resolved release runtime dependencies, and a manual CI workflow. Core hardening was verified at `14bdfb8e911a0422cd48f72a5bea7f37e2433674`; the latest branded application source is `db3d9e76acd266aec5613cf8838c98b56bb7e96e`. Clean JVM/lint/build gates passed; device/OAuth/signing gates remain open. This assessment does not declare the app production ready or authorize public distribution.

The 0.1.2/code3 delta adds generated dashboard/launcher branding and six rendering/accessibility unit tests, not a new financial/storage/network implementation. The current clean run passed 157 tests across 46 suites, zero lint issues, instrumentation compilation and both APK builds. Targeted logging/embedded-secret scans again returned no matches; the 107 selected runtime packages are unchanged. A separate branding review finding was repaired RED→GREEN; this is not a second security review or penetration test. [Branding evidence](branding/review-and-verification.md)

## Runtime dependency and advisory evidence

The runtime inventory is generated from Gradle's selected `releaseRuntimeClasspath` artifacts, including transitive dependencies. It contains **107 distinct Maven package/version pairs**. Constraint-only entries such as the Compose BOM, test/debug dependencies, build plugins, the Gradle distribution, Android OS components, and the core-library desugaring tool are outside this runtime-artifact inventory. It records selected versions rather than assuming the version catalog is the dependency graph.

- [Complete resolved inventory](security/runtime-inventory-2026-10-09.json)
- [Complete OSV response mapped to each package](security/osv-runtime-2026-10-09.json)
- [Core-candidate OSV response](security/osv-runtime-2026-10-10.json):107 selected packages,0 known affected packages,0 differences from the reviewed inventory. SHA-256:`5fb631c32205e9054c633da75b0bcca946085e8aed4c390be9439d0e7ae2bfdb`.
- [Branded-candidate OSV response](security/osv-runtime-2026-10-11.json): checked at `2026-10-10T19:04:28.221424+00:00` (2026-10-11 in Dhaka),107 selected packages,0 known affected packages,0 differences from the reviewed inventory. SHA-256:`6d5ab9c931832e9d4addd89580337b84558356e0701d66e4508364ecf10b94b1`.

SHA-256 of the reviewable snapshots: inventory `293a9d5e50edc33fcebf0697da35ade98ce69ff12eedfda4b91eae40c676e325`; OSV response `c762002c1fa909462399295966bb6370031d35d9151edea3a2daa1a5fce35148`. These normalized JSON copies preserve the original data; their hashes differ from the original generated files' formatting.

The real HTTPS request to `https://api.osv.dev/v1/querybatch` completed at **2026-10-09T17:26:43.825512+00:00**. OSV returned no known advisories for all 107 package/version pairs. Only public Maven coordinates and versions were sent; no ledger, backup, account identifier, OAuth token, key, or application source was sent. The endpoint accepts batched version-specific queries; it returns advisory identifiers rather than a full vulnerability analysis. [OSV querybatch API](https://google.github.io/osv.dev/post-v1-querybatch/)

A separate scanner control queried `org.apache.logging.log4j:log4j-core:2.14.1`, which is **not an app dependency**. OSV returned seven advisory identifiers, and the scanner exited 1 as required. This verifies that a finding blocks the gate. Network/protocol errors exit 2; an empty inventory or paginated/incomplete response cannot pass. A clean OSV response is limited by database coverage and timing and is not proof that these packages, the app, or the Android device contain no vulnerabilities.

To repeat the exact runtime gate locally:

```powershell
.\gradlew.bat --no-daemon --console=plain --init-script scripts/runtime-inventory.init.gradle :app:writeReleaseRuntimeInventory
python scripts/check-runtime-advisories.py app/build/reports/dependencies/release-runtime.json app/build/reports/dependencies/osv-runtime.json
```

Re-run after any dependency, variant, or release configuration change and before distributing a new release. Compare the new selected packages with this dated snapshot and review any advisory for affected versions and applicability. Do not mark an unavailable advisory service as a passing check.

## Source security boundaries

Source-only scans of `app/src/main` found zero production logging-call matches (`Log`, `println`, stack-trace printing, Timber, Crashlytics, Sentry) and zero embedded-secret-pattern matches (common API/token formats, PEM private keys, and literal OAuth client secrets). The secret-pattern scan also covered Gradle configuration. This is a targeted pattern scan, not a complete entropy scan or an examination of Git history, signing files, binaries, or external accounts. Scans printed matching file names only and did not read financial databases or user backups.

Observed boundaries:

- The app source requests Internet, notifications, and boot-completed permissions. The merged APK also includes WorkManager wake-lock/network-state/foreground-service permissions and AndroidX's app-scoped signature receiver permission. It does not request broad storage, contacts, location, camera, or microphone access. Cleartext traffic is disabled.
- The launcher activity is exported for Android launch. The reminder receiver is private. The exported boot receiver accepts only `ACTION_BOOT_COMPLETED`; it reschedules reminders rather than accepting ledger mutations from arbitrary intent extras. Notification action pending intents are immutable.
- Room stores the ledger in app-private storage, with explicit v1-to-v2 and v2-to-v3 migrations and no destructive migration fallback. The local database is not independently encrypted by the application; protection relies on the Android sandbox, device storage encryption, and the owner's device lock.
- Explicit backups use authenticated AES-GCM envelopes, random salts/nonces, and password-based derivation. The local background-backup password cache stores ciphertext in private preferences and uses an Android Keystore key. It does not require interactive authentication for every key use, allowing opted-in background backup. This should not be described as an app lock or protection against a compromised/rooted device.
- New backups use 600,000 PBKDF2-HMAC-SHA256 iterations; older authenticated v1 snapshots retain their recorded derivation count (10,000–1,000,000) for compatibility. Constructor/import bounds prevent creating an envelope the app cannot decrypt and bound expensive derivation. The new default follows the SHA256 work-factor recommendation in [OWASP password-storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html); it is not a backup-security certification. A long unique passphrase and minimum-device performance testing remain necessary.
- Google authorization requests only `drive.appdata`. Access tokens are passed in memory to HTTPS requests and are not persisted by the app's source. The selected folder flow uses Android's Storage Access Framework.
- Release shrinking/minification is enabled. A successful unsigned release build is a compilation/minification gate, not proof of owner-controlled release signing or device behavior.
- Local-data deletion cancels recurring work and app notifications; worker lookup/posting now uses the shared deletion boundary. Backup readiness is separate from destination/account selection and is persisted only after a successful encrypted write. These final-review repairs have failing-then-passing regressions, not physical-device certification.

The corrected modern cloud/device-transfer rules and legacy backup rules explicitly exclude all nine Android domains, including database, files, preferences, external and device-protected storage. Android documents that `allowBackup=false` can leave manufacturer-specific device transfers enabled, so declared exclusions do not replace real-device checks. [Android backup and extraction rules](https://developer.android.com/identity/data/autobackup)

An API compatibility blocker was repaired in `BackupCrypto`: the platform factory `PBKDF2WithHmacSHA256` is documented as available from API 26, while the application declares API 24 support. The fallback implements the same SHA-256 PBKDF2 derivation using HmacSHA256, preserving the authenticated v1 backup format. Published vectors and Unicode/native-provider parity are exercised in JVM tests, along with Robolectric API 24/25 checks. Instrumentation sources compile, but actual Android execution is still required: JVM/Robolectric results do not establish real-device provider behavior. [Android SecretKeyFactory supported algorithms](https://developer.android.com/reference/javax/crypto/SecretKeyFactory), [RFC 8018 PBKDF2](https://www.rfc-editor.org/rfc/rfc8018#section-5.2)

## Proposed CI gates and cost boundary

`.github/workflows/android-release-gates.yml` is manually dispatched and defaults to skipping all jobs unless the owner explicitly confirms the included Actions allowance and a no-overage spending limit. It has no push, pull-request, scheduled, publication, signing, or deployment trigger. No workflow was run during this assessment.

The build job runs JVM tests, debug/release lint, debug assembly, R8-minified unsigned release assembly, instrumentation compilation, runtime resolution, and the OSV gate. After build success, instrumentation runs serially on API 24 and API 36 Google APIs x86_64 emulators. Both exact image IDs were verified in Google's public [SDK image repository](https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml). The pair covers the declared minimum and target OS; it does not substitute for testing intermediate OS changes or physical devices.

Only official GitHub actions are used and pinned to exact commit SHAs verified from their public repositories:

| Action | Release | Pinned commit |
| --- | --- | --- |
| `actions/checkout` | v6.0.3 | `df4cb1c069e1874edd31b4311f1884172cec0e10` |
| `actions/setup-java` | v6.0.1 | `de7274f081f381c8f8158605e0321c36c376e2e6` |

Checkout does not persist credentials; the token has `contents: read` only. The workflow uses no owner secrets, dependency cache, uploaded artifacts, or APK publication. Jobs have 30-minute limits, one emulator at a time, bounded boot/test waits, and emulator teardown. CI does not obtain Google OAuth credentials or a release keystore. SHA pinning and minimum token permissions follow [GitHub's secure-use guidance](https://docs.github.com/en/actions/reference/security/secure-use). Android tools use the official runner's documented `ANDROID_HOME` at `/usr/local/lib/android/sdk` and SDK commands rather than an unpinned emulator action. [Ubuntu 24.04 runner inventory](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md), [Android emulator commands](https://developer.android.com/studio/run/emulator-commandline), [Android acceleration options](https://developer.android.com/studio/run/emulator-acceleration)

Static validation passed with checksum-verified `actionlint` 1.7.12, Python syntax compilation, and Bash syntax checks for all seven workflow run blocks. These are configuration checks; GitHub-hosted execution and both instrumentation jobs remain unverified.

This private repository's standard hosted jobs consume the owner's included allowance. Workflow YAML cannot inspect or enforce the account's remaining allowance or spending policy. The owner must verify enough free minutes for up to three bounded 30-minute jobs, shared account usage, and a stop-usage budget/no paid overage before enabling the input. No billing setting was changed. No caches or artifacts are stored by this workflow; logs and job summaries preserve the small result record. [GitHub Actions allowance and billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions)

## Required owner gates before production distribution

| Gate | Required evidence | Current status |
| --- | --- | --- |
| Final hardening validation | Fresh whole-suite results, lint, minified build, and both emulator runs against the final change set | 157 JVM tests and both lint/build gates passed; instrumentation compiled but no device; hosted CI not run |
| Minimum OS crypto | API 24/25 encrypted round trip, wrong-password rejection, and cross-version compatibility of existing backups | Format-compatible fallback implemented; vectors/Unicode parity and Robolectric checks passed; actual Android execution required |
| Backup/restore lifecycle | All backup/import/delete/worker operations share the intended serialization boundary; restore failure retains the ledger; platform extraction exclusions verified | Focused57-test backup/settings GREEN plus full suite; physical-device checks outstanding |
| Owner-controlled signing | Release keystore, backed-up key/recovery procedure, signed APK signature verification, clean install/update test, and SHA-256 distribution record | Owner signing material and signed candidate absent |
| Google OAuth/Drive | Registered Android package and signing-certificate client, consent/test users, real sign-in/denial/revocation/reconnect, upload and clean-device restore | Owner setup/test account absent; fake transport tests are insufficient |
| Device behavior | Physical minimum/current OS, process death, reboot/background backup/reminders, denied permissions, Bangla/English layouts, large ledger, uninstall/reinstall/folder and Drive restore | Physical-device evidence absent |
| Private release access and cost | Explicit approved recipients/distribution path; free allowance plus stopped overage verified before any CI dispatch | No publication or billing action performed |
| Privacy and security claims | Docs match final manifest/storage/extraction behavior and clearly distinguish encrypted backups from local database protection | Core review completed; four Important issues repaired. Branding review added one repaired Important issue; current157-test suite green. No certification/penetration-test claim |

Both the older 0.1.0 alpha asset and the current locally built 0.1.2 candidate are test artifacts. Missing owner OAuth configuration, release signing, device checks, or execution evidence remain incomplete release gates. See the [exact candidate/checksum report](testing/release-test-report.md).
