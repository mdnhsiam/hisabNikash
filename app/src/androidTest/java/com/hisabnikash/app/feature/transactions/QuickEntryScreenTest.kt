package com.hisabnikash.app.feature.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuickEntryScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun paymentMethodsAndSinglePrimarySaveActionAreVisible() {
        var submitted = false
        composeRule.setContent {
            HisabTheme {
                QuickEntryScreen(
                    state = QuickEntryState(
                        categories = listOf(Category("expense_food", CategoryType.EXPENSE, "Food", "খাবার", 0, true)),
                    ),
                    onSubmit = { submitted = true },
                )
            }
        }

        composeRule.onNodeWithText("Cash").assertIsDisplayed()
        composeRule.onNodeWithText("Cash").performClick()
        composeRule.onNodeWithText("B  Bank").assertIsDisplayed()
        composeRule.onNodeWithText("b  bKash").assertIsDisplayed()
        composeRule.onNodeWithText("N  Nagad").assertIsDisplayed()
        composeRule.onNodeWithText("Save transaction").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(submitted) }
    }
}
