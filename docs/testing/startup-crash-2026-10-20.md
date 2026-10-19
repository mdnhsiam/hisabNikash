# Cold-start navigation crash

## Completed repair and review

Source41a2b5976ed095fd00e7f0363f99c58f625d3380: deferred list, version0.1.3/code4 and regressions. Focused RED5tests/4failures became GREEN5/5; complete clean162tests/48suites,zero failures/errors/skips,zero lint issues; debug/test/minified unsigned release APKs built. Independent read-only review:ready to merge scoped repair,no Critical/Important/Minor findings. [Publication/artifact evidence](release-test-report.md).

### Rulings made

- Preserve the user's no-intermediate-plan-question preference for this approved bounded repair; no new feature design. Cost:review scope remains the repair,not expanded product design.
- Native apply-patch task notes replace shell bookkeeping helpers after filesystem permission errors and interruption. Tests/review/evidence remain required; cost:less automated bookkeeping,not weaker app gates.
- Reviewer declined physical Nothing Phone3a equivalence/launch:no device/log; retain unverified gate. Cost:another cause on that phone remains possible.
- Reviewer declined instrumentation execution:compiled/built only,not device-pass. Cost:Android-runtime differences remain untested.
- Reviewer declined actual installed update/data preservation:matching package/certificate,higher code and unchanged storage establish intended compatibility,not installed proof. No uninstall/data clear performed; cost:owner must verify update.
- Reviewer delegated artifact/release checks:root verified signature/certificate equality,size/hash,alignment,version and GitHub digest; confirmed authorized0.1.2 asset withdrawal via metadata. Cost:artifact checks do not prove device behavior.
- Reviewer declined production/OAuth/architecture/security certification:remain open; OSV limited to107packages/database coverage. Cost:wider release cannot be certified from this fix.

Deferred minors:none. Device/OAuth/restore/owner signing remain known external release gates,not waived findings.

## Original diagnosis

User report: Nothing Phone 3a, Android16; 0.1.2 closes before the dashboard. Local full-launch diagnostics on modeled API35 and36 reproduce `NullPointerException` at `HisabNavHost.kt:243`, dereferencing a null bottom-navigation route. No physical phone is connected; equivalence to the phone's actual stack remains unverified.

`HisabNavHost` first references `Route.Dashboard.path`. Initializing that singleton initializes its `Route` superclass, whose eager companion list reads the still-uninitialized `Dashboard.INSTANCE`. The list retains null. The existing route tests access the list first and mask the opposite initialization order; isolated screen tests do not exercise the full launch. Compiled bytecode confirms this cycle.

Repair requirements: defer list creation; retain all five route identities/order/labels; add initialization-order-isolated and full real-Activity regressions; preserve ledger/preferences/schema/signing identity; bump the replacement to 0.1.3/code4. Publish a private test APK, then withdraw the 0.1.2 APK asset only, retaining source/tag/history and a local rollback copy. Do not clear phone data or uninstall the app. Production/OAuth/device certification remains out of scope.
