package com.hisabnikash.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStartupInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launch_and_recreation_reach_the_real_dashboard() {
        assertDashboard()
        compose.activityRule.scenario.recreate()
        assertDashboard()
    }

    private fun assertDashboard() {
        compose.onNodeWithText(compose.activity.getString(R.string.dashboard_title)).assertIsDisplayed()
        for (label in listOf(R.string.nav_dashboard, R.string.nav_transactions, R.string.nav_reports, R.string.nav_more)) {
            compose.onNodeWithText(compose.activity.getString(label)).assertIsDisplayed()
        }
    }
}
