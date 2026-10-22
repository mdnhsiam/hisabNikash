package com.hisabnikash.app.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.hisabnikash.app.ui.theme.HisabTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class MoreShortcutsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `backup language and savings are discoverable without opening generic settings`() {
        var settingsOpened = 0
        var savingsOpened = 0
        compose.setContent { HisabTheme { MoreScreen({ savingsOpened++ }, { settingsOpened++ }) } }
        compose.onNodeWithText("Backup and restore").performScrollTo().performClick()
        compose.onNodeWithText("Language").performScrollTo().performClick()
        compose.onNodeWithText("Savings goals").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(2, settingsOpened)
            assertEquals(1, savingsOpened)
        }
    }
}
