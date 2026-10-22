package com.hisabnikash.app.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TypographyRenderingTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `financial headings use distinct display typography from explanatory prose without clipping Bangla`() {
        var heading: TextLayoutResult? = null
        var prose: TextLayoutResult? = null
        composeRule.setContent {
            HisabTheme {
                Column {
                    Text("৳২৬,৫০০", style = MaterialTheme.typography.headlineLarge,
                        onTextLayout = { heading = it })
                    Text("আপনার হিসাব অফলাইনেও নিরাপদে সংরক্ষিত থাকে।",
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = { prose = it })
                }
            }
        }
        composeRule.runOnIdle {
            val amount = requireNotNull(heading)
            val description = requireNotNull(prose)
            assertNotEquals(amount.layoutInput.style.fontFamily, description.layoutInput.style.fontFamily)
            assertTrue(amount.size.height > 0)
            assertFalse(amount.hasVisualOverflow)
            assertFalse(description.hasVisualOverflow)
        }
    }
}
