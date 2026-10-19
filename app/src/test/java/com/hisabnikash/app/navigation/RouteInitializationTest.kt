package com.hisabnikash.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RouteInitializationTest {
    @Test fun `each destination can initialize before the navigation list in a cold process`() {
        for (destination in listOf("Dashboard", "Transactions", "Reports", "Savings", "Settings")) {
            assertRoutesAfterFirstClass("$ROUTE_CLASS\$$destination")
        }
    }

    @Test fun `navigation list can initialize before its destinations in a cold process`() {
        assertRoutesAfterFirstClass(ROUTE_CLASS)
    }

    private fun assertRoutesAfterFirstClass(firstClass: String) {
        // Reload the real compiled route hierarchy only. A warmed test JVM must
        // not hide the singleton/superclass initialization cycle from cold start.
        val parent = Route::class.java.classLoader!!
        val loader = object : ClassLoader(parent) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name != ROUTE_CLASS && !name.startsWith("$ROUTE_CLASS\$")) {
                    return super.loadClass(name, resolve)
                }
                synchronized(this) {
                    val loaded = findLoadedClass(name) ?: run {
                        val resource = name.replace('.', '/') + ".class"
                        val bytes = requireNotNull(parent.getResourceAsStream(resource)).use { it.readBytes() }
                        defineClass(name, bytes, 0, bytes.size)
                    }
                    if (resolve) resolveClass(loaded)
                    return loaded
                }
            }
        }
        Class.forName(firstClass, true, loader)
        val route = Class.forName(ROUTE_CLASS, true, loader)
        val companion = route.getField("Companion").get(null)
        val entries = companion.javaClass.getMethod("getBottomRoutes").invoke(companion) as List<*>
        entries.forEach { assertNotNull("null navigation entry after $firstClass initializes first", it) }
        assertEquals(
            "all five ordered destinations must remain usable after $firstClass initializes first",
            listOf("dashboard", "transactions", "reports", "savings", "settings"),
            entries.map { route.getMethod("getPath").invoke(it) },
        )
    }

    companion object {
        private const val ROUTE_CLASS = "com.hisabnikash.app.navigation.Route"
    }
}
