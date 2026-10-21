# Closed-test build report

Date: 2026-10-21 (Asia/Dhaka). Candidate: HisabNikash 0.1.4, version code 5. Verified application source: `1d5cdb357e0772b3467aa56b057e55b1e9ca0669`. Later documentation commits do not change the binary.

## UI and privacy refresh

The owner approved the simplified mint/forest design with offline Bangla typography, four primary destinations, expandable native transaction entry and hidden-by-default Home details. [Implementation, research and independent review](minimal-ui-review-2026-10-21.md).

Home monthly amounts and recent descriptions are absent from composition/accessibility semantics until the eye action reveals them. Reveal resets on pause/background or a fresh Home/Activity composition. This is display privacy, not authentication, screenshot blocking or encryption. Reports and Transactions remain deliberately accessible by navigation.

Quick entry preserves drafts across resizing and Activity recreation; explicit discard clears input without changing the ledger. Unsaved system process-death recovery is not promised. Existing description/category/amount validation, one-off or reusable categories, duplicate checks and backup behavior remain. Date/note start collapsed; an invalid date keeps its recovery field visible.

## Executed final gates

- Full clean Gradle command: `clean testDebugUnitTest lintDebug lintRelease compileDebugAndroidTestKotlin assembleDebug assembleDebugAndroidTest assembleRelease --init-script scripts/runtime-inventory.init.gradle :app:writeReleaseRuntimeInventory`.
- BUILD SUCCESSFUL in 3m27s; 149 tasks (147 executed, 2 up-to-date).
- 49 suites, **171 tests, zero failures/errors/skips**. Includes existing finance/SQLite/migration/backup/branding coverage and new Home privacy/accessibility/lifecycle, route cold-start, drilldown, real Activity recreation and draft/date regressions.
- Debug and release lint: **zero issues** in both XML reports.
- Instrumentation source compilation and test APK build: passed; actual Android instrumentation **not executed**.
- Debug and R8-minified unsigned release APK builds: passed. Redundant lint-vital tasks skipped because release lint ran separately.
- Non-blocking kapt processor-option warnings remain; no warning suppression was added.
- Independent read-only review: no Critical/Important findings; one Minor invalid-date recovery issue reproduced RED (1 test/1 failure), repaired and covered by the complete final green suite.
- First full run passed 170 tests but failed lint on indentation and an unused old resource. Both were repaired before the successful clean rerun, not suppressed.
- Runtime inventory: 107 selected Maven packages; zero differences from the reviewed inventory. [OSV response](../security/osv-runtime-2026-10-21.json) returned zero known affected packages. Snapshot SHA256: `7ad052329f58474b27e3f3aba82c202eb7bee9dc44927fd3ccd4d388221380aa`. Not proof of no vulnerabilities.
- Targeted filename-only production logging/embedded-secret scans: no matches; not a full security audit.
- APK checks: package `com.hisabnikash.app`, version 0.1.4/code5, min API24, target API36; debug v2 signature verifies with the previous certificate. Both APKs pass16K zip alignment. Release is non-debuggable and unsigned as expected.

No implementation changes followed the final clean run. Native Robolectric renders of hidden/revealed Home, Bangla at200% text, More and partial/expanded entry were inspected. These are not phone screenshots or a usability study.

A fresh handoff rerun of the complete `testDebugUnitTest` task (task-level `--rerun`, not cached test execution) also passed in1m9s on the unchanged application source after the browser upload failure.

## Artifact and publication status

- Built APK: `app/build/outputs/apk/debug/app-debug.apk`
- Owner's local copy: `C:/Users/user/Documents/new Try/HisabNikash-0.1.4-test.apk`
- Size: **16,822,054 bytes**
- SHA256: `7db0dbd8b5d17bb5edef1d12506011fe6f50d00df3dcfab279f7c7df9ea85192`
- Public debug certificate SHA256: `2db8c68906df844b71c7c03d186b5a8916f880548033d6d693fa0aa1610078c2`
- Public debug certificate SHA1: `1b49ea24c0846d6c3ea477c0008cab56134577fc` (test OAuth only).
- Source commit1d5cdb3 was pushed to GitHub main.

**GitHub binary publication is incomplete.** The browser file-chooser upload failed and its release-form tab was no longer available. A fresh GitHub releases read confirms no0.1.4 release/asset. The prepared title/notes/tag selection were not proof of publication. No second upload was attempted after this failure. The existing [0.1.3 release](https://github.com/mdnhsiam/hisabNikash/releases/tag/v0.1.3-alpha.1) remains intact and does **not** contain this redesigned UI. Do not present its download as0.1.4.

Install the verified local0.1.4 APK over the existing app; do not uninstall or clear data. Matching package/certificate and a higher version support intended compatibility, but actual phone update/ledger preservation is not verified. If Android rejects the update, stop rather than uninstalling unbacked-up data. No phone data was changed by this task. No previous APK was deleted.

## Open release gates

Fresh adb inventory is empty; emulator acceleration reports no installed hypervisor driver. Actual phone IME/drag/TalkBack/OEM rendering, minimum-OS full sheet/font interaction, installed update, real folder/Drive restore, Google OAuth and owner-controlled production signing remain unverified. No system security setting or paid CI was enabled. [Manual checklist](manual-release-checklist.md), [production readiness](../production-readiness-2026-10-09.md).

This debug-signed candidate is not production certification or a zero-bugs guarantee.
