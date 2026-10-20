package com.hisabnikash.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.inspector.WindowInspector
import java.io.File

/** Native Robolectric renders, not emulator/device screenshots or PixelCopy. API35 tests only. */
internal fun captureUi(name: String) {
    val windows = WindowInspector.getGlobalWindowViews().filter { it.isShown && it.width > 0 && it.height > 0 }
    check(windows.isNotEmpty())
    val first = windows.first()
    val image = Bitmap.createBitmap(first.width, first.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    windows.forEach { view ->
        val position = IntArray(2)
        view.getLocationOnScreen(position)
        canvas.save()
        canvas.translate(position[0].toFloat(), position[1].toFloat())
        view.draw(canvas)
        canvas.restore()
    }
    val output = File("build/reports/ui-refresh/$name.png")
    output.parentFile!!.mkdirs()
    output.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    image.recycle()
}
