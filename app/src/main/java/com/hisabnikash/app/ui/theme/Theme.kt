package com.hisabnikash.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HisabColors = lightColorScheme(
    primary = Forest,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Mint,
    background = WarmBackground,
    onBackground = Ink,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Ink,
    error = Expense,
)

@Composable
fun HisabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HisabColors,
        typography = HisabTypography,
        content = content,
    )
}
