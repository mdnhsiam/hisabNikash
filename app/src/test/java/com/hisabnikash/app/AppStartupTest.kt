package com.hisabnikash.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
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

    @Test @Config(sdk = [35])
    fun `revealed income opens a labeled drilldown and view all restores the normal list`() {
        compose.onNodeWithText(compose.activity.getString(R.string.income)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.view_all)).assertIsDisplayed().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.search_transactions)).assertIsDisplayed()
    }

    @Test @Config(sdk = [35])
    fun `more keeps secondary features reachable and draft survives expand collapse and cancelled dismiss`() {
        compose.onNodeWithText(compose.activity.getString(R.string.nav_more)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.budget_shortcut)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.settings_title)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { captureUi("more-bn") }
        compose.onNodeWithText(compose.activity.getString(R.string.nav_dashboard)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.add_transaction)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.amount_bdt)).assertIsDisplayed()
        val amountBounds = compose.onNode(hasSetTextAction() and hasText(compose.activity.getString(R.string.amount_bdt))).fetchSemanticsNode().boundsInRoot
        assertTrue("The partial sheet must expose a usable amount field, not just its clipped label", amountBounds.height >= 64 * compose.activity.resources.displayMetrics.density)
        compose.runOnIdle { captureUi("entry-partial-bn") }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.expand_entry)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.amount_bdt)).performTextInput("250")
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.collapse_entry)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.expand_entry)).performClick()
        compose.onNodeWithText("250").assertIsDisplayed()
        compose.runOnIdle { captureUi("entry-expanded-bn") }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("250").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.close_entry)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.discard_entry_title)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.keep_editing)).performClick()
        compose.onNodeWithText("250").assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.close_entry)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.discard_entry)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.add_transaction)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.expand_entry)).performClick()
        compose.onNodeWithText("250").assertDoesNotExist()
    }

    private fun assertDashboard() {
        compose.onNodeWithText(compose.activity.getString(R.string.dashboard_title)).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.monthly_remainder)).assertIsDisplayed()
        for (label in listOf(R.string.nav_dashboard, R.string.nav_transactions, R.string.nav_reports, R.string.nav_more)) {
            compose.onNodeWithText(compose.activity.getString(label)).assertIsDisplayed()
        }
    }

    @Test @Config(sdk = [35])
    fun `backup shortcut opens its controls without manual settings scrolling`() {
        compose.onNodeWithText(compose.activity.getString(R.string.nav_more)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.backup_title)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.choose_backup_folder)).assertIsDisplayed()
    }
}
