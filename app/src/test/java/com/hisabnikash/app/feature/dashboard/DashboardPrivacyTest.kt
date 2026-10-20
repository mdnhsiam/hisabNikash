package com.hisabnikash.app.feature.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.hisabnikash.app.core.model.PaymentMethod
import com.hisabnikash.app.core.model.Transaction
import com.hisabnikash.app.core.model.TransactionType
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Catches initial exposure, visual-only masks and reveal state leaking across app backgrounding. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class DashboardPrivacyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val restoration by lazy { StateRestorationTester(compose) }

    private fun open() {
        val transactions = (1..5).map { index ->
            Transaction("$index", TransactionType.EXPENSE, 25_000, "Private purchase $index", "food",
                paymentMethod = PaymentMethod.CASH, occurredAtEpochMillis = 1L, createdAtEpochMillis = 1L)
        }
        val summary = emptyDashboardSummary(MonthKey(2026, 10)).copy(
            incomeMinor = 4_500_000, expenseMinor = 1_850_000, netSavingsMinor = 2_650_000,
            recentTransactions = transactions,
        )
        restoration.setContent { HisabTheme { DashboardScreen(DashboardState(summary)) } }
    }

    @Test fun `initial dashboard contains no private transaction text even in unmerged accessibility tree`() {
        open()
        compose.onNodeWithText("Private purchase 1", useUnmergedTree = true).assertDoesNotExist()
        compose.onAllNodes(hasText("26,500", substring = true), useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Show dashboard details").assertIsDisplayed()
    }

    @Test fun `explicit reveal shows three recent entries and hide removes their semantics`() {
        open()
        compose.onNodeWithContentDescription("Show dashboard details").performClick()
        compose.onNodeWithText("Private purchase 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Private purchase 3").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Private purchase 4", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Hide dashboard details").performScrollTo().performClick()
        compose.onNodeWithText("Private purchase 1", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun `background and recreation require a fresh explicit reveal`() {
        open()
        compose.onNodeWithContentDescription("Show dashboard details").performClick()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithText("Private purchase 1", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Show dashboard details").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Private purchase 1", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Show dashboard details").assertIsDisplayed()
    }
}
