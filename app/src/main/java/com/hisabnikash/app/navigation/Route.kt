package com.hisabnikash.app.navigation

import androidx.annotation.StringRes
import com.hisabnikash.app.R

sealed class Route(
    val path: String,
    val labelKey: String,
    @param:StringRes val labelRes: Int,
) {
    data object Dashboard : Route("dashboard", "nav_dashboard", R.string.nav_dashboard)
    data object Transactions : Route("transactions", "nav_transactions", R.string.nav_transactions)
    data object Reports : Route("reports", "nav_reports", R.string.nav_reports)
    data object Savings : Route("savings", "nav_savings", R.string.nav_savings)
    data object Settings : Route("settings", "nav_settings", R.string.nav_settings)
    data object More : Route("more", "nav_more", R.string.nav_more)

    companion object {
        // A destination can initialize its Route superclass before its own
        // INSTANCE exists. Do not capture those singletons in Route's initializer.
        val bottomRoutes: List<Route> by lazy { listOf(Dashboard, Transactions, Reports, More) }
    }
}
