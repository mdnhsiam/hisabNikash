package com.hisabnikash.app.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Decorative native glyphs; the parent navigation item supplies the localized accessible label. */
@Composable
fun NavigationGlyph(route: Route) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(24.dp)) {
        val unit = size.width / 24
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(color, Offset(x1 * unit, y1 * unit), Offset(x2 * unit, y2 * unit), 2 * unit)
        when (route) {
            Route.Dashboard -> {
                for (x in listOf(3f, 14f)) for (y in listOf(3f, 14f))
                    drawRect(color, Offset(x * unit, y * unit), Size(7 * unit, 7 * unit), style = Stroke(2 * unit))
            }
            Route.Transactions -> { line(3f, 7f, 20f, 7f); line(16f, 3f, 20f, 7f); line(16f, 11f, 20f, 7f)
                line(4f, 17f, 21f, 17f); line(4f, 17f, 8f, 13f); line(4f, 17f, 8f, 21f) }
            Route.Reports -> { line(4f, 21f, 21f, 21f); line(6f, 19f, 6f, 12f); line(12f, 19f, 12f, 4f); line(18f, 19f, 18f, 8f) }
            Route.Savings -> { drawRect(color, Offset(3 * unit, 5 * unit), Size(18 * unit, 15 * unit), style = Stroke(2 * unit))
                line(3f, 9f, 21f, 9f); drawCircle(color, 2 * unit, Offset(16 * unit, 15 * unit)) }
            Route.Settings -> { for (y in listOf(5f, 12f, 19f)) line(3f, y, 21f, y)
                for ((x, y) in listOf(8f to 5f, 16f to 12f, 8f to 19f)) drawCircle(color, 3 * unit, Offset(x * unit, y * unit), style = Stroke(2 * unit)) }
        }
    }
}
