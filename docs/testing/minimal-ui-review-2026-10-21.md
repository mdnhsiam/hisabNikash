# Minimal native UI refresh

Scope approved by the owner: the simplified mint/forest preview, a native partial/full transaction sheet, Home / Transactions / Reports / More navigation, and bank-style hidden Home details. This is a bounded presentation change to existing flows, not debt persistence or Google OAuth implementation.

## Implemented behavior

- Home starts hidden. The accessible eye action reveals monthly recorded income-minus-expense and at most three recent entries. Hiding removes actual financial text and descriptions from composition/accessibility semantics. Pause/background, leaving Home and Activity recreation require a fresh reveal; it is not an authentication lock or screenshot protection.
- The income and expense summaries open type- and Dhaka-month-filtered transactions. View all and the Transactions navigation action clear filters and stale search. The monthly remainder is explicitly not an account balance or a safe-to-spend forecast.
- Four primary destinations; More preserves access to existing budgets/savings and settings, including backup, language, categories and reminders. No nonfunctional debt button was added.
- Quick entry remains a native modal sheet with partial/expanded anchors, drag handle and labeled accessible resize/close controls. Form content scrolls with IME insets. Nonempty drafts require confirmation before close/back/swipe dismissal; cancel retains the draft, explicit discard clears it. Financial fields, local date draft and disclosure state survive Activity recreation; system process-death recovery is not promised for unsaved drafts.
- Expense/income identification, amount/description/category validation, Cash/Bank/bKash/Nagad choices, custom one-off/reusable categories and duplicate protection remain. Date/note start collapsed, but an invalid date cannot hide its recovery field while Save is disabled.
- Typography uses bundled Noto Sans Bengali, no network font dependency. The official Google Fonts TTF SHA256 is `dcd42978094e584a849c84a51450eeac40c8826057d566ea6d4b9627a403a05a`. Full OFL and copyright are packaged under `assets/licenses/noto_sans_bengali_OFL.txt` and readable in More. The original ledger/checkmark logo remains.
- Version 0.1.4/code 5. No database, schema, runtime dependency or signing configuration changes.

## Research basis

The owner requested bank-app privacy inspiration. Starling's [published balance-hiding feature](https://www.starlingbank.com/blog/banking-app-features-inspired-by-starling-customers/) demonstrates explicit balance hiding; this app uses a visible accessible eye action instead of requiring a discoverable long press. Task-focused hierarchy and one primary action were informed by [Android navigation guidance](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns), [Money Lover's published quick-entry flow](https://moneylover.me/) and [Copilot's separate transaction workflows](https://help.copilot.money/en/articles/9554412-transactions-tab-overview). No competitor screen or identity was copied, and no exhaustive hands-on competitor audit is claimed.

## Review and regression evidence

Final clean gate: BUILD SUCCESSFUL in 3m27s, 149 tasks (147 executed, 2 up-to-date). 49 suites / 171 tests, zero failures/errors/skips; debug/release lint each zero issues; instrumentation compilation/test APK and debug/minified unsigned release APK builds passed. Instrumentation was not executed. Non-blocking kapt processor-option warnings remain. The final APK verifies with the same v2 debug signing certificate as0.1.3 and both APKs pass16K alignment. Runtime inventory unchanged (107 packages); the dated OSV refresh returned no known affected packages, not proof of no vulnerabilities. Source did not change after this clean run.

A fresh-context independent read-only reviewer examined the full uncommitted UI diff, new components/tests, relevant existing finance/navigation/date code and font/license. No Critical or Important finding was confirmed. One Minor finding: an invalid date could be collapsed, hiding the explanation for disabled Save. A real production-composable interaction regression reproduced the missing recovery field (1 test/1 failure); the implementation keeps that section visible while invalid. Final gate results are recorded in the current [release report](release-test-report.md).

Earlier RED evidence includes initial private text exposure, missing navigation behavior, sheet loss across real Activity recreation, a no-op discard control and a no-op month/type filter control. The focused repair run passed before the final clean gates. The first complete run passed 170 tests but failed lint on indentation and an unused old monthly-balance resource. Both were repaired rather than suppressed; the final rerun also includes the date regression.

Native Robolectric view-hierarchy renders are generated at `app/build/reports/ui-refresh/` for hidden/revealed Home, Bangla at 200% text, More and partial/expanded entry. They are not physical-phone screenshots. A compact amount-field assertion is not certification of real IME or gesture behavior.

## Reviewer limitations retained as explicit gates

- Final suite/lint/artifact completion: owned by the implementer's final gate, not inferred from the review. Exact results/checksum are in the release report.
- Physical-device IME, drag, TalkBack and OEM rendering: not certified by source review or Robolectric. Device inventory is empty; no hypervisor driver is installed. No system security setting was changed.
- Minimum API24 full sheet/font rendering: startup coverage exists; all interactions still require actual Android testing.
- System process-death draft recovery: not implemented or promised; save important entries. Saved ledger persistence is unchanged.
- Font provenance: the implementer downloaded the recorded bytes from the official `google/fonts` repository, retained the license and recorded the digest; the reviewer did not independently compare upstream bytes.
- Owner signing, installed update/ledger preservation and OAuth/Drive restore remain separate unverified gates. Matching package/certificate and a higher code establish intended update compatibility, not an installed-device result.
- Visual inspection is performed against the regenerated native captures by the implementer; not represented as a device usability study.

No Critical/Important finding was deferred. The single Minor finding was repaired with its regression. No production, zero-bugs or complete-security certification is asserted.
