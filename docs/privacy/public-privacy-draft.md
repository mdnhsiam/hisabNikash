# HisabNikash privacy notice — owner-review draft

Prepared2026-10-23. This file is not a published privacy URL or legal certification. Review the developer identity, contact and actual release behavior before publishing. Intended support/privacy contact (owner approved): mail.nhsiam@gmail.com.

## বাংলা সংক্ষেপ

হিসাব নিকাশ ব্যক্তিগত আয়-ব্যয়ের অফলাইন Android অ্যাপ। আপনার হিসাব, ক্যাটাগরি, বাজেট, সঞ্চয় ও রিমাইন্ডার ফোনে সংরক্ষিত থাকে। Google সংযোগ বাধ্যতামূলক নয়। অ্যাপের নিজস্ব developer server, বিজ্ঞাপন বা analytics নেই; অ্যাপ আপনার আর্থিক রেকর্ড developer-এর কাছে আপলোড করে না।

আপনি চাইলে নির্দিষ্ট ফোন-ফোল্ডারে অথবা নিজের Google Drive-এ password-encrypted backup রাখতে পারেন। Google Drive সংযোগে অ্যাপ শুধু নিজের app-data area ব্যবহারের অনুমতি চায়, পুরো Drive নয়। এই backup সাধারণ Drive file-list-এ দেখা যায় না; অ্যাপ থেকে restore করা হয়। Google নিজস্ব privacy policy অনুযায়ী তাদের সেবা পরিচালনা করে।

ফোনের স্থানীয় database আলাদাভাবে অ্যাপ দিয়ে encrypt করা নয়; Android-এর app isolation, device encryption ও screen lock-এর ওপর নির্ভর করে। Developer আপনার backup password উদ্ধার করতে পারে না। অ্যাপ uninstall করলে private local data হারাতে পারে; external backup সফল হয়েছে নিশ্চিত না হয়ে uninstall করবেন না।

স্থানীয় হিসাব মুছলে আপনার নির্বাচিত folder/Drive-এর backup নিজে থেকে মুছে যায় না। সেগুলো আলাদা করে মুছতে হয়। Backup কেবল snapshot; একাধিক ফোনে একসঙ্গে live sync নয়। কোনো support email-এ password, token বা আর্থিক backup পাঠাবেন না।

## English notice

### Data on your device

HisabNikash stores transactions, descriptions/notes, categories, payment-method selections, budgets, savings goals/allocations and recurring reminders in its Android app-private database. Preferences store settings such as language and optional backup configuration. No developer-operated financial-data backend, advertising SDK or analytics service is included in this release. App use does not require an account. Home totals are visible by default; only recent entry details require reveal. This is not an app lock.

The local database is not separately encrypted by the app. It relies on Android app isolation, device encryption and your device lock. It does not protect against an already-unlocked, rooted or compromised device.

### Optional backup and Google data

Only when you choose backup does the app write an encrypted snapshot to your selected phone folder or directly to your own Google Drive app-data area. Financial records are not routed through a developer server. Google authorization requests only `https://www.googleapis.com/auth/drive.appdata`, to create/read/update this app's backup data, not access your general Drive files. Account selection and consent are handled by Google. Google may process connection/account information under its own [privacy policy](https://policies.google.com/privacy).

Backups use password-derived authenticated encryption before upload. Keep your password safely; the developer cannot recover it. Access tokens are kept in memory. If you explicitly enable background backup, an Android Keystore-protected password cache supports that operation; plaintext passwords are not stored in preferences. Internet permission supports optional Google backup; notification/boot permissions support optional recurring reminders, not bank account access. No contacts, microphone, camera or broad-storage permission is requested by the app.

### Retention, deletion and control

Local data remains until you delete it or Android removes app-private storage (for example on uninstall). External backup files may survive uninstall but must be successfully created and later accessible; uninstall survival is not guaranteed for every folder provider/device. Disconnecting Google Drive or deleting local records does not delete existing external snapshots. Disable background backup before deleting external snapshots to avoid recreation. Delete chosen-folder copies with the file provider. Hidden Drive app data can be removed in Google Drive Settings → Manage apps → HisabNikash → Options → Delete hidden app data. You may revoke Google authorization in your Google account.

Restore replaces current local records only after validation/decryption and captures an encrypted recovery copy first. Verify restore on a spare installation before relying on it. Backups are snapshots, not conflict-resolving live multi-device sync. Google/account storage quotas and service availability apply.

### Contact and changes

For privacy/support questions contact mail.nhsiam@gmail.com. Do not send encryption passwords, Google tokens or backup contents. Information you choose to email is handled by your and the developer's email providers and is separate from the app's local ledger. The developer must review this notice if the app's data flows change and keep the published notice consistent with the distributed version.
