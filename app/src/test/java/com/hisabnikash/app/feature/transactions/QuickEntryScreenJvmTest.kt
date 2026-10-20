package com.hisabnikash.app.feature.transactions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import com.hisabnikash.app.core.model.Category
import com.hisabnikash.app.core.model.CategoryType
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuickEntryScreenJvmTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `optional date and note stay collapsed until requested`() {
        composeRule.setContent {
            HisabTheme { QuickEntryScreen(state = QuickEntryState(), onSubmit = {}) }
        }
        composeRule.onNodeWithText("Note (optional)").assertDoesNotExist()
        composeRule.onNodeWithText("Date and note").performScrollTo().performClick()
        composeRule.onNodeWithText("Note (optional)").performScrollTo().assertIsDisplayed()
    }

    @Test fun `invalid date cannot hide its recovery field behind a collapsed disclosure`() {
        composeRule.setContent {
            HisabTheme { QuickEntryScreen(state = QuickEntryState(), onSubmit = {}) }
        }
        composeRule.onNodeWithText("Date and note").performScrollTo().performClick()
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performScrollTo().performTextReplacement("not-a-date")
        composeRule.onNodeWithText("Save transaction").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Date and note").performScrollTo().performClick()
        composeRule.onNodeWithText("Date (YYYY-MM-DD)").performScrollTo().assertIsDisplayed()
            .performTextReplacement("2026-10-21")
        composeRule.onNodeWithText("Save transaction").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `payment choices and primary save action are usable`() {
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
