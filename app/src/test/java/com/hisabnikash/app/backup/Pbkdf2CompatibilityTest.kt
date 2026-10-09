package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.crypto.Pbkdf2Sha256
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25])
class Pbkdf2CompatibilityTest {
    @Test fun `SHA256 compatibility derivation matches RFC 7914 section 11 vectors`() {
        assertArrayEquals(hex("55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"), Pbkdf2Sha256.derive("passwd".toCharArray(), "salt".toByteArray(), 1))
        assertArrayEquals(hex("4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56"), Pbkdf2Sha256.derive("Password".toCharArray(), "NaCl".toByteArray(), 80_000))
    }

    @Test fun `fallback agrees with native SHA256 for unicode empty and long passwords`() {
        val salt = ByteArray(16) { it.toByte() }
        listOf("", "ascii", "বাংলা-গোপন", "emoji-\uD83D\uDD10", "x".repeat(200)).forEach { password ->
            val spec = PBEKeySpec(password.toCharArray(), salt, 10_000, 256)
            val native = try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
            assertArrayEquals(native, Pbkdf2Sha256.derive(password.toCharArray(), salt, 10_000))
        }
    }

    @Test fun `backup encrypted round trip runs at minimum supported Android versions`() {
        val crypto = BackupCrypto(10_000)
        val clear = "fixture".toByteArray()
        assertArrayEquals(clear, crypto.decrypt(crypto.encrypt(clear, "password9".toCharArray()), "password9".toCharArray()))
    }

    private fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
