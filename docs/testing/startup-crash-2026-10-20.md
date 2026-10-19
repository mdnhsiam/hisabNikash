# Cold-start navigation crash

User report: Nothing Phone 3a, Android16; 0.1.2 closes before the dashboard. Local full-launch diagnostics on modeled API35 and36 reproduce `NullPointerException` at `HisabNavHost.kt:243`, dereferencing a null bottom-navigation route. No physical phone is connected; equivalence to the phone's actual stack remains unverified.

`HisabNavHost` first references `Route.Dashboard.path`. Initializing that singleton initializes its `Route` superclass, whose eager companion list reads the still-uninitialized `Dashboard.INSTANCE`. The list retains null. The existing route tests access the list first and mask the opposite initialization order; isolated screen tests do not exercise the full launch. Compiled bytecode confirms this cycle.

Repair requirements: defer list creation; retain all five route identities/order/labels; add initialization-order-isolated and full real-Activity regressions; preserve ledger/preferences/schema/signing identity; bump the replacement to 0.1.3/code4. Publish a private test APK, then withdraw the 0.1.2 APK asset only, retaining source/tag/history and a local rollback copy. Do not clear phone data or uninstall the app. Production/OAuth/device certification remains out of scope.
