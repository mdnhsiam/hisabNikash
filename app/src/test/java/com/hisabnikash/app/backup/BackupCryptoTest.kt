package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.crypto.BackupAuthenticationException
import com.hisabnikash.app.backup.crypto.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupCryptoTest {
    private val crypto = BackupCrypto(pbkdf2Iterations = 10_000)

    @Test
    fun `random salt and nonce make identical snapshots produce different ciphertext`() {
        val bytes = "private finance data".encodeToByteArray()

        val first = crypto.encrypt(bytes, "correct horse".toCharArray())
        val second = crypto.encrypt(bytes, "correct horse".toCharArray())

        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.nonceBase64, second.nonceBase64)
        assertNotEquals(first.ciphertextBase64, second.ciphertextBase64)
        assertArrayEquals(bytes, crypto.decrypt(first, "correct horse".toCharArray()))
    }

    @Test(expected = BackupAuthenticationException::class)
    fun `wrong password is rejected`() {
        val envelope = crypto.encrypt("secret".encodeToByteArray(), "right".toCharArray())
        crypto.decrypt(envelope, "wrong".toCharArray())
    }

    @Test(expected = BackupAuthenticationException::class)
    fun `tampered ciphertext is rejected`() {
        val envelope = crypto.encrypt("secret".encodeToByteArray(), "right".toCharArray())
        val tampered = envelope.copy(ciphertextBase64 = envelope.ciphertextBase64.reversed())
        crypto.decrypt(tampered, "right".toCharArray())
    }
}
