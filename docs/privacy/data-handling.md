# Data handling and release security

HisabNikash is offline-first. Transactions, categories, budgets, goals and recurring items are stored in the app's private Room database. No analytics, advertisements, developer server, or embedded service credential is used. The local database is not separately encrypted by the app; it relies on Android app isolation, device encryption and the device lock. A rooted, compromised or already-unlocked device is outside that protection boundary.

## Home display privacy

As of0.1.5/0.1.6, Home shows monthly remaining/income/expense immediately, without a global eye button. Only recent transaction details start hidden. Their local show/hide control removes private entry text from composition and accessibility semantics rather than placing an overlay over it. Leaving Home, recreating the Activity, or pausing/backgrounding the app requires a fresh reveal of recent entries, not totals. Reports and the transaction list remain intentionally accessible through explicit navigation.

This is shoulder-surfing protection, not authentication, encryption, a biometric app lock, screenshot blocking, or protection against a compromised/unlocked phone. It does not erase financial records. Unsaved entry drafts survive sheet resizing and Activity recreation, not guaranteed system process death; save important entries before leaving.

## Backup flows

- Phone-folder backup is opt-in through Android's Storage Access Framework. The user chooses the folder and the app retains access only to that selection.
- Google Drive backup is opt-in, requests only `drive.appdata`, and stores the encrypted envelope in the signed-in user's private app-data area.
- Destination selection/Google connection is not proof of a saved snapshot. Settings keeps setup incomplete until the first encrypted backup succeeds. Changing folders or losing a queued backup password requires completing setup again; reconnecting solely for restore does not hide the backup warning.
- Snapshot contents are encrypted before either transport with an authenticated, password-derived key. Plaintext passwords and access tokens are not written to logs or preferences. An Android Keystore-protected ciphertext password cache is stored in app-private preferences only to support explicitly enabled background backup. Tokens remain in memory.
- Restore validates/decrypts completely before replacing current data. Wrong passwords, corrupt payloads, unsupported versions, or invalid references leave current data unchanged. Before a valid replacement, the app writes an encrypted on-device recovery copy that can undo the last restore.
- The manifest and extraction rules exclude app-private data from Android platform cloud backup/device transfer; only the user-selected encrypted flows are intended. Manufacturer-specific behavior still requires real-device verification.

## Manifest and network review

- App-declared permissions: Internet for optional Drive backup, notification delivery and boot-completed rescheduling. The merged APK also includes WorkManager wake-lock/network-state/foreground-service permissions and AndroidX's app-scoped signature permission for private dynamic receivers. No broad storage, contacts, location, microphone or camera permission.
- App-owned exported components: launcher activity and the boot receiver required for the system boot broadcast; the reminder receiver is private. AndroidX/Google libraries add their own manifest components; inspect the merged release manifest, not just the source manifest.
- Cleartext traffic is disabled.
- Drive transport uses HTTPS and only the `drive.appdata` OAuth scope. Tokens remain in memory.
- Release builds enable R8 shrinking/obfuscation with the optimized Android defaults. Room, Kotlin serialization, WorkManager and Google Play Services contribute their consumer rules; no reflection-only app model requires a custom keep rule.
- Source scan found no embedded API keys, OAuth client secrets, private keys, sensitive logging calls, or stack-trace printing.

## Deletion and limitations

“Delete all local data” removes the local ledger and private backup settings, cancels scheduled recurring work and clears this app's posted notifications. Reminder lookup/posting shares the deletion boundary so an in-flight worker cannot post a deleted record afterward. It does not delete backup files already owned by the user in a selected folder or Google Drive. A forgotten encryption password cannot be recovered. The developer cannot read or recover user-owned backups.

Disable phone-folder backup or disconnect Drive before manually deleting the corresponding external snapshot, otherwise an enabled background job may create another copy. Phone snapshots are removed using the Files app in the selected folder. Drive hidden app data can be removed in the same Google account's Drive Settings → Manage apps → HisabNikash → Options → Delete hidden app data. The app must not imply that disconnecting or deleting local data also deletes remote snapshots. [Google Drive storage management](https://support.google.com/drive/answer/6374270?hl=en)

Backups are snapshots, not concurrent live multi-device synchronization. Production distribution requires the recorded device, authorization and owner-signing gates in the [readiness report](../production-readiness-2026-10-09.md).
