package com.hisabnikash.app.ui

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
class LauncherBrandingTest {
    private fun icon() = ApplicationProvider.getApplicationContext<Context>().let { context ->
        // AGP's unit-test manifest omits the application's icon. Resolve our actual
        // declaration against the packaged resources instead of mocking an icon.
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val manifest = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val application = manifest.getElementsByTagName("application").item(0) as org.w3c.dom.Element
        val resource = application.getAttributeNS("http://schemas.android.com/apk/res/android", "icon").removePrefix("@")
        val (type, name) = resource.split('/')
        val id = context.resources.getIdentifier(name, type, context.packageName)
        require(id != 0) { "The production launcher resource is missing: $resource" }
        context.getDrawable(id)!!
    }

    @Test @Config(sdk = [24])
    fun `minimum OS resolves the generated mark over its launcher background`() {
        val drawable = icon()
        assertTrue("API24 needs the legacy layered logo, not an adaptive-only resource", drawable is LayerDrawable)
        val mark = (drawable as LayerDrawable).getDrawable(1)
        assertTrue("the packaged generated mark must decode", mark is BitmapDrawable)
        val bitmap = (mark as BitmapDrawable).bitmap
        assertTrue("the source margins must stay transparent", android.graphics.Color.alpha(bitmap.getPixel(0, 0)) == 0)
    }

    @Test @Config(sdk = [26])
    fun `adaptive launcher resolves its foreground and background on API26`() {
        val drawable = icon()
        assertTrue("the manifest must select the adaptive resource", drawable is AdaptiveIconDrawable)
        assertNotNull((drawable as AdaptiveIconDrawable).foreground)
        assertNotNull(drawable.background)
    }

    @Test @Config(sdk = [35])
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `modern launcher has a monochrome layer for themed icons`() {
        val drawable = icon()
        assertTrue(drawable is AdaptiveIconDrawable)
        assertNotNull("themed launchers require our supplied monochrome layer", (drawable as AdaptiveIconDrawable).monochrome)
        val rendered = android.graphics.Bitmap.createBitmap(216, 216, android.graphics.Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 216, 216)
        drawable.draw(android.graphics.Canvas(rendered))
        val output = File("build/reports/branding/launcher.png")
        requireNotNull(output.parentFile).mkdirs()
        output.outputStream().use { rendered.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(sdk = [35])
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun `foreground stays within the circular adaptive safe zone at compact and enlarged sizes`() {
        for (size in listOf(108, 324)) {
            val foreground = (icon() as AdaptiveIconDrawable).foreground
            val rendered = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            foreground.setBounds(0, 0, size, size)
            foreground.draw(android.graphics.Canvas(rendered))
            // Android guarantees a centered 66dp-diameter circle, not a 66dp square.
            val center = size / 2.0
            val radius = size * 33.0 / 108.0
            var visible = 0
            var outsideSafeZone = 0
            for (y in 0 until size) for (x in 0 until size) {
                if (android.graphics.Color.alpha(rendered.getPixel(x, y)) > 32) {
                    visible++
                    val dx = x + 0.5 - center
                    val dy = y + 0.5 - center
                    if (dx * dx + dy * dy > radius * radius) outsideSafeZone++
                }
            }
            assertTrue("the packaged foreground must actually render", visible > 100)
            org.junit.Assert.assertEquals("a launcher must not clip the mark at size $size", 0, outsideSafeZone)
        }
    }
}
