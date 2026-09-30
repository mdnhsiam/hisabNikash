package com.hisabnikash.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {
    @Test
    fun `bottom routes expose unique stable paths`() {
        val routes = Route.bottomRoutes

        assertEquals(5, routes.size)
        assertEquals(routes.size, routes.map(Route::path).toSet().size)
        assertEquals(
            setOf("dashboard", "transactions", "reports", "savings", "settings"),
            routes.map(Route::path).toSet(),
        )
    }

    @Test
    fun `bottom routes expose localized string keys`() {
        val keys = Route.bottomRoutes.map(Route::labelKey)

        assertEquals(
            setOf(
                "nav_dashboard",
                "nav_transactions",
                "nav_reports",
                "nav_savings",
                "nav_settings",
            ),
            keys.toSet(),
        )
        assertTrue(keys.none(String::isBlank))
    }
}
