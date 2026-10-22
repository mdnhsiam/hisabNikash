# HisabNikash setup status — 2026-10-23 (Asia/Dhaka)

## Scope and current state

Owner approved the bounded 0.1.5/code6 UI adjustment: monthly totals visible from entry; only recent entries require local reveal; larger mint summary with stronger financial typography; functional More shortcuts. Existing native partial/expanded QuickEntrySheet and all financial/storage behavior remain unchanged. The subsequent owner-approved 0.1.6/code7 candidate packages Hind Siliguri for display/headings/titles/labels and Noto Sans Bengali for body prose. Both fonts and their OFL licenses are offline assets; no font provider or runtime dependency was added.

Published GitHub test download is now [0.1.5-alpha.1](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.5-alpha.1), with remote size/digest verified against the local APK. [Current verification report](ui-polish-2026-10-23.md). No existing release APK was overwritten.

## Remaining setup and acceptance gates

| Gate | What is needed | Current evidence / limit |
| --- | --- | --- |
| Font choice and UI acceptance | Inspect approved Hind/Noto mix on the actual phone at normal/200% text, small width and landscape | Owner selected Hind Siliguri with Noto prose. Browser font specimens and Robolectric renders are not physical-device screenshots. |
| Physical phone update | USB-debugging connection approved on the owner phone; update without uninstall/data clear; launch and verify existing ledger | Latest adb inventory empty. Nothing Phone3a/Android16 results must be recorded, not inferred from JVM tests. |
| Google authorization | Test actual Google consent/cancel/revoke on the phone | Project/API, branding, Android test client, one owner test account and only drive.appdata declaration are configured. Publishing status remains External/Testing. No phone consent or real remote backup has been executed by the agent. |
| Backup and restore | Create encrypted folder/Drive snapshot; restore on a spare/test installation; test wrong password, permission revocation and low storage | Fake transport/JVM coverage does not establish provider/OAuth/device correctness. Never uninstall unbacked-up real data. |
| Device UI/accessibility | Partial/full sheet drag, keyboard insets, TalkBack, rotation, long amounts, Bangla/English, notification actions and month boundary | Native Robolectric renders/tests are useful but not physical-device certification. |
| Production signing | Owner creates and securely backs up a production signing key; register that actual certificate for OAuth | Debug-signed test APK only. Different signing keys cannot update each other; verify external restore before any migration. Never commit the key/password. |
| Production/minimum OS gates | Execute actual API24/current Android instrumentation, signed minified build and manual checklist | Compiled instrumentation is not executed instrumentation. No paid CI or billing enabled. |
| Distribution/privacy | Owner decides test recipients and, later, production distribution; publish privacy/support information before wider release | Private GitHub prerelease is controlled testing, not Play Store/public production approval. Native offline APK needs no web hosting/server. |

“Perfect” or “zero bugs” cannot be certified. Open gates remain open even with a green build.

## Typography research

Android's [standard typography guidance](https://m1.material.io/style/typography.html) describes Roboto/Noto as platform standards; current [Compose Material3 typography](https://developer.android.com/develop/ui/compose/designsystems/material3) uses named type roles. This does not mean all banking apps use one font. Legibility depends on size, real/synthesized weight, contrast, line height and wrapping, not the family alone.

Official Google Fonts alternatives: [Noto Sans Bengali](https://github.com/google/fonts/tree/main/ofl/notosansbengali), [Hind Siliguri metadata](https://raw.githubusercontent.com/google/fonts/main/ofl/hindsiliguri/METADATA.pb), [Anek Bangla metadata](https://raw.githubusercontent.com/google/fonts/main/ofl/anekbangla/METADATA.pb). Hind offers static 400/500/600/700 weights; Anek has variable width/weight. They are OFL families. A final alternative requires bundling its font/license and regression testing, not adding an online font dependency to the app.

## Google Drive OAuth owner runbook

This app authorizes optional Drive backup, not a mandatory app login. Keep offline use available. The current implementation uses Google Play services AuthorizationClient, without a developer backend or a client secret in the APK.

1. Owner selects/creates a **dedicated Google Cloud project**. Record project ID and ownership; no password/token/key should be shared in chat or committed. Creating OAuth access, accepting terms or enabling billing requires owner action/explicit approval. Do not attach a paid billing account or raise quotas for this test.
2. In that project enable **Google Drive API**. Set up Google Auth Platform **Branding** (HisabNikash and an owner-controlled support/developer contact), **Audience** (External/Testing for ordinary personal Gmail use), and add the actual test Google accounts. See [Google's consent-screen instructions](https://developers.google.com/workspace/guides/configure-oauth-consent).
3. In **Data Access**, request only `https://www.googleapis.com/auth/drive.appdata`. It is a non-sensitive scope for the user's app-data area; it does not grant general Drive access. Backups are hidden from the regular Drive file list. Do not substitute a service account, developer-owned Drive, broad `drive` scope or developer storage. [Drive app-data documentation](https://developers.google.com/workspace/drive/api/guides/appdata).
4. In **Clients**, create an **Android** OAuth client for package `com.hisabnikash.app` with the actual installed APK's signing SHA1. Test certificate: `1B:49:EA:24:C0:84:6D:6C:3E:A4:77:C0:00:8C:AB:56:13:45:77:FC`. Re-verify with apksigner on the candidate APK before registration. This is a public certificate fingerprint, not a private key. Production needs a separate registration for its real signing certificate; Play App Signing uses the app-signing certificate, not merely the upload key. [Client creation](https://developers.google.com/workspace/guides/create-credentials), [Android authorization](https://developer.android.com/identity/authorization), [certificate authentication](https://developers.google.com/android/guides/client-auth).
5. Do not add a web client/client secret/Firebase configuration just because a web sign-in tutorial asks for one. This code requests Drive authorization through AuthorizationClient and does not request backend server access. Inspect any proposed change before broadening the design.
6. On the authorized test phone: More → Backup and restore → Connect Google Drive; approve the Google consent flow yourself. Cancel/deny once and confirm offline use works; then authorize, set a backup password, explicitly run **Back up now**, and verify success status. Connecting alone is not a backup. Preserve the password outside the app.
7. On a spare/test installation, reconnect the **same Google account and app identity**, restore, compare transaction/month totals and confirm wrong-password failures preserve existing data. Exercise revocation, disconnect/reconnect, offline recovery and folder permission failures. Never clear/uninstall the owner's unbacked-up ledger for a test.
8. Testing authorizations can expire after seven days; handle reauthorization rather than claiming permanent background access. Google's test-user limits and expiry are documented in [Manage App Audience](https://support.google.com/cloud/answer/15549945). Do not publish the OAuth app to production merely to bypass test expiry.

### Cost constraint

As checked on 2026-10-23, Google documents standard Drive API use at no additional cost, but also describes planned quota-overage charges later in 2026. This is **not** an unconditional forever-free guarantee. Keep billing/paid overage disabled and inspect the selected project's current quotas before wider rollout. User Drive storage remains subject to that user's account quota. [Current Drive usage/pricing policy](https://developers.google.com/workspace/drive/api/guides/limits).

Read-only inspection of this project's Drive API Quotas & System Limits showed 1,000,000 units/minute/project and 325,000 units/minute/user; the Console also displays previous request quotas of 12,000/minute and 12,000/minute/user. Displayed project usage was zero. No quota edits, alert policy, billing attachment or paid increase was made. These rate limits are not a promise of unlimited free storage or future pricing.

## Owner-requested Cloud follow-up

After the verified UI release was published, the owner explicitly requested a new project through Chrome instead of reusing an existing project. Created **HisabNikash**, project ID `hisabnikash-512420`, project number `421798698404`, in the currently signed-in owner's existing organization. Console's finished creation notification and selected-project Welcome page were verified. The project's Billing → Linked account page explicitly reports **This project has no billing account** and **This project is not linked to a billing account**. No extra IAM grants, paid services, billing link or account credential changes were made by the agent. OAuth client/consent and remote backup are not yet complete.

Google Drive API was enabled and its API/Service Details page verified `Status: Enabled`. No billing account, card, free-trial payment setup or paid quota increase was added. The owner approved the support/developer contact email and personally completed the policy agreement/branding creation. The Console confirmed `OAuth configuration created!`.

After specific owner approval, created Android client `HisabNikash Android test` for `com.hisabnikash.app` and the installed-candidate certificate SHA1 above (reverified with apksigner). Console confirmed `OAuth client created`. Public client ID: `421798698404-r4b8ikhhs7r8do9mng5272b752shhps6.apps.googleusercontent.com`; this is an identifier, not a client secret. No client JSON or secret is required in the APK for the current AuthorizationClient flow. Saved only the approved owner email as a test user (Console: 1 test user/100 cap). Saved only `https://www.googleapis.com/auth/drive.appdata` in Data Access; sensitive/restricted lists remain empty and Console confirmed `Data access changes saved!`. No production publication or wider audience/access was enabled.

Cloud configuration is not phone authorization or proof of a working backup. The user's actual Google data consent remains an owner action. Use the registered test account with the current debug-signed APK, allow Console propagation (the UI warns 5 minutes to a few hours), then follow the backup/restore runbook. Free-only constraints remain mandatory; no guarantee of unlimited future free quotas is made.
