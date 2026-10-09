package com.hisabnikash.app.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hisabnikash.app.backup.crypto.BackupCrypto
import com.hisabnikash.app.backup.crypto.Pbkdf2Sha256
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupCryptoCompatibilityInstrumentedTest {
    @Test fun backup_round_trip_uses_supported_sha256_derivation() {
        val crypto = BackupCrypto(10_000)
        val password = "বাংলা-\uD83D\uDD10".toCharArray()
        val clear = "fixture only".toByteArray()
        assertArrayEquals(clear, crypto.decrypt(crypto.encrypt(clear, password), password))
    }

    @Test fun compatibility_derivation_matches_rfc_7914() {
        val expected = "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56".chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expected, Pbkdf2Sha256.derive("Password".toCharArray(), "NaCl".toByteArray(), 80_000))
    }
}
