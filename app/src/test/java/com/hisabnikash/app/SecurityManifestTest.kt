package com.hisabnikash.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityManifestTest {
    @Test fun `manifest permits HTTPS networking for opt in Drive backup`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"false\""))
    }
}
