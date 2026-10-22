package com.hisabnikash.app.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import com.hisabnikash.app.captureUi
import com.hisabnikash.app.core.time.MonthKey
import com.hisabnikash.app.ui.theme.HisabTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardBrandingTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun `dashboard shows the accessible app identity without replacing the financial heading`() {
        compose.setContent { HisabTheme { DashboardScreen(DashboardState(emptyDashboardSummary(MonthKey(2026, 10)))) } }
        compose.onNodeWithText("HisabNikash").assertIsDisplayed()
        compose.onNodeWithText("Your month at a glance").assertIsDisplayed()
        compose.runOnIdle { captureUi("home-hidden-en") }
        compose.onNodeWithText("Show transactions").performScrollTo().performClick()
        compose.runOnIdle { captureUi("home-revealed-en") }
        // PixelCopy's forced frame wait is unavailable in Robolectric's offscreen
        // window. Draw the actual laid-out view hierarchy using native graphics.
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            val output = File("build/reports/branding/dashboard.png")
            requireNotNull(output.parentFile).mkdirs()
            output.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test @Config(qualifiers = "bn")
    fun `Bangla identity remains readable with large text`() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f),
            ) {
                HisabTheme { DashboardScreen(DashboardState(emptyDashboardSummary(MonthKey(2026, 10)))) }
            }
        }
        compose.onNodeWithText("হিসাব নিকাশ").assertIsDisplayed()
        compose.onNodeWithText("এই মাসের হিসাব এক নজরে").assertIsDisplayed()
        compose.runOnIdle { captureUi("home-hidden-bn-large") }
    }
}
