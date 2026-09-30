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

    companion object {
        val bottomRoutes: List<Route> = listOf(Dashboard, Transactions, Reports, Savings, Settings)
    }
}
