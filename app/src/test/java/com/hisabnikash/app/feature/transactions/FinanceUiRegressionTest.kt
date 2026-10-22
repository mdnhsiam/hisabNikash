package com.hisabnikash.app.feature.transactions

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hisabnikash.app.core.model.*
import com.hisabnikash.app.feature.recurring.*
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FinanceUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val food = Category("expense_food", CategoryType.EXPENSE, "Food", "খাবার", 0, true)
    private val salary = Category("income_salary", CategoryType.INCOME, "Salary", "বেতন", 0, true)
    private val expense = Transaction("bill", TransactionType.EXPENSE, 1000, "Lunch", food.id,
        paymentMethod = PaymentMethod.CASH, occurredAtEpochMillis = 1, createdAtEpochMillis = 1)

    @Test fun `history amounts use emphasized display font rather than prose defaults`() {
        compose.setContent { HisabTheme { TransactionListScreen(
            TransactionListState(listOf(expense), categories = mapOf(food.id to food)),
            onQueryChange = {}, onUpdate = { false }, onDelete = { _, _ -> false }) } }
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNode(hasText("−", substring = true), useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult,
        ) { action -> action(layouts) }
        compose.runOnIdle {
            org.junit.Assert.assertEquals(
                com.hisabnikash.app.ui.theme.HisabTypography.titleSmall.fontFamily,
                layouts.single().layoutInput.style.fontFamily,
            )
            org.junit.Assert.assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold,
                layouts.single().layoutInput.style.fontWeight)
        }
    }

    @Test fun `duplicate warning identifies the suspected transaction`() {
        compose.setContent { HisabTheme { DuplicateWarningDialog(expense, {}, {}) } }
        compose.onNodeWithText("Lunch").assertExists()
    }

    @Test fun `reports expose income and expense totals`() {
        val month = com.hisabnikash.app.core.time.MonthKey(2025, 1)
        val report = com.hisabnikash.app.domain.FinanceCalculator.monthlyReport(emptyList(), month)
        compose.setContent { HisabTheme { com.hisabnikash.app.feature.reports.ReportsScreen(
            com.hisabnikash.app.feature.reports.ReportsState(month, report), {}, {}) } }
        compose.onNodeWithText("Income").assertExists()
        compose.onNodeWithText("Expense").assertExists()
        compose.onNodeWithText("Daily totals").assertExists()
    }

    private fun history() {
        compose.setContent { HisabTheme { TransactionListScreen(
            TransactionListState(listOf(expense), categories = listOf(food, salary).associateBy { it.id }),
            onQueryChange = {}, onUpdate = { false }, onDelete = { _, _ -> false }) } }
        compose.onNodeWithText("Lunch").performClick()
    }

    @Test fun `failed edit preserves the dialog and edited text`() {
        history()
        compose.onNodeWithText("Description").performTextReplacement("Changed lunch")
        compose.onNodeWithText("Save", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Edit transaction").assertIsDisplayed()
        compose.onNodeWithText("Changed lunch").assertExists()
    }

    @Test fun `changing type requires a matching category selection`() {
        history()
        compose.onNodeWithText("Income").performClick()
        compose.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test fun `recurring review exposes amount and payment method`() {
        val item = RecurringItem("bill", TransactionType.EXPENSE, 1000, "Electricity", food.id,
            PaymentMethod.CASH, nextDueAtEpochMillis = 100, createdAtEpochMillis = 0)
        compose.setContent { HisabTheme { RecurringScreen(RecurringState(listOf(item)), categories = listOf(food),
            notificationPermissionGranted = true, onRequestPermission = {}, onCreate = { true },
            onConfirm = { _, _ -> true }, onReviewConfirm = { _, _, _ -> true }, onSkip = { _, _ -> true }, onDisable = { true }) } }
        compose.onNodeWithText("Open and review").performClick()
        compose.onNodeWithText("Amount (BDT)").assertExists()
        compose.onNodeWithText("Cash").assertExists()
    }
}
