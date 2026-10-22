package com.hisabnikash.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hisabnikash.app.R

// Packaged font: no network font provider, account, or connectivity required.
private val ReadingFont = FontFamily(Font(R.font.noto_sans_bengali))
private val DisplayFont = FontFamily(
    Font(R.font.hind_siliguri_regular, FontWeight.Normal),
    Font(R.font.hind_siliguri_medium, FontWeight.Medium),
    Font(R.font.hind_siliguri_semibold, FontWeight.SemiBold),
    Font(R.font.hind_siliguri_bold, FontWeight.Bold),
)
private fun TextStyle.hisab() = copy(fontFamily = DisplayFont, letterSpacing = 0.sp)
private fun TextStyle.reading() = copy(fontFamily = ReadingFont, letterSpacing = 0.sp)
private val baseline = Typography()
val HisabTypography = Typography(
    displayLarge = baseline.displayLarge.hisab(),
    displayMedium = baseline.displayMedium.hisab(),
    displaySmall = baseline.displaySmall.hisab(),
    headlineLarge = baseline.headlineLarge.hisab().copy(fontSize = 34.sp, lineHeight = 48.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = baseline.headlineMedium.hisab().copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = baseline.headlineSmall.hisab(),
    titleLarge = baseline.titleLarge.hisab().copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.hisab().copy(fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleSmall = baseline.titleSmall.hisab().copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = baseline.bodyLarge.reading().copy(lineHeight = 28.sp),
    bodyMedium = baseline.bodyMedium.reading().copy(lineHeight = 24.sp),
    bodySmall = baseline.bodySmall.reading().copy(lineHeight = 21.sp),
    labelLarge = baseline.labelLarge.hisab().copy(lineHeight = 24.sp),
    labelMedium = baseline.labelMedium.hisab().copy(lineHeight = 20.sp),
    labelSmall = baseline.labelSmall.hisab(),
)
