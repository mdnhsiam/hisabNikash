# Data handling and release security

Hisab is offline-first. Transactions, categories, budgets, goals and recurring items are stored in the app's private Room database. No analytics, advertisements, developer server, or embedded service credential is used.

## Backup flows

- Phone-folder backup is opt-in through Android's Storage Access Framework. The user chooses the folder and the app retains access only to that selection.
- Google Drive backup is opt-in, requests only `drive.appdata`, and stores the encrypted envelope in the signed-in user's private app-data area.
- Snapshot contents are encrypted before either transport with an authenticated, password-derived key. Passwords and access tokens are not written to logs or preferences. A protected local password cache is used only to support explicitly enabled background backup.
- Restore validates/decrypts completely before replacing current data. Wrong passwords, corrupt payloads, unsupported versions, or invalid references leave current data unchanged. Before a valid replacement, the app writes an encrypted on-device recovery copy that can undo the last restore.
- Android platform cloud backup/device transfer is disabled; only the user-selected encrypted flows are supported.

## Manifest and network review

- Permissions: notification delivery and boot-completed rescheduling only. No broad storage, contacts, location, microphone or camera permission.
- Exported components: launcher activity and the boot receiver required for the system boot broadcast; the reminder receiver is private.
- Cleartext traffic is disabled.
- Drive transport uses HTTPS and only the `drive.appdata` OAuth scope. Tokens remain in memory.
- Release builds enable R8 shrinking/obfuscation with the optimized Android defaults. Room, Kotlin serialization, WorkManager and Google Play Services contribute their consumer rules; no reflection-only app model requires a custom keep rule.
- Source scan found no embedded API keys, OAuth client secrets, private keys, sensitive logging calls, or stack-trace printing.

## Deletion and limitations

“Delete all local data” removes the local ledger and private backup settings. It does not delete backup files already owned by the user in a selected folder or Google Drive. A forgotten encryption password cannot be recovered. The developer cannot read or recover user-owned backups.
