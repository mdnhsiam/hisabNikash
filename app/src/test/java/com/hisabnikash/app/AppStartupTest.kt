package com.hisabnikash.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35, 36], application = HisabApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppStartupTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            // Robolectric does not run manifest providers like Android does.
            // Supply the real initializer, not a fake production dependency.
            val app = ApplicationProvider.getApplicationContext<HisabApplication>()
            androidx.startup.AppInitializer.getInstance(app)
                .initializeComponent(androidx.work.WorkManagerInitializer::class.java)
        }

        override fun after() {
            ApplicationProvider.getApplicationContext<HisabApplication>().container.database.close()
        }
    }).around(compose)

    @Test fun `real application launch and recreation show dashboard and all navigation destinations`() {
        assertDashboard()
        compose.activityRule.scenario.recreate()
        assertDashboard()
    }

    @Test @Config(sdk = [24]) @GraphicsMode(GraphicsMode.Mode.LEGACY)
    fun `minimum supported OS launches the real dashboard`() {
        assertDashboard()
    }

    private fun assertDashboard() {
        compose.onNodeWithText(compose.activity.getString(R.string.dashboard_title)).assertIsDisplayed()
        for (label in listOf(R.string.nav_dashboard, R.string.nav_transactions, R.string.nav_reports, R.string.nav_savings, R.string.nav_settings)) {
            compose.onNodeWithText(compose.activity.getString(label)).assertIsDisplayed()
        }
    }
}
