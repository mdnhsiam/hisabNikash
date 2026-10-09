package com.hisabnikash.app.backup

import com.hisabnikash.app.backup.crypto.BackupAuthenticationException
import com.hisabnikash.app.backup.crypto.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.Security
import javax.crypto.SecretKeyFactory
import org.junit.Assert.assertTrue

class BackupCryptoTest {
    private val crypto = BackupCrypto(pbkdf2Iterations = 10_000)

    @Test(expected = IllegalArgumentException::class)
    fun `encryption cannot be configured to produce an envelope above supported restore work factor`() {
        BackupCrypto(pbkdf2Iterations = 1_000_001)
    }

    @Test fun `new snapshots use at least 600000 SHA256 iterations while legacy v1 snapshots still restore`() {
        val password = "fixture-password".toCharArray()
        val clear = "fixture".toByteArray()
        val current = BackupCrypto()
        val newEnvelope = current.encrypt(clear, password)
        assertTrue("new snapshots need the updated work factor", newEnvelope.kdfIterations >= 600_000)
        listOf(10_000, 210_000).forEach { iterations ->
            val legacy = BackupCrypto(iterations).encrypt(clear, password)
            assertArrayEquals(clear, current.decrypt(legacy, password))
        }
    }

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

    @Test
    fun `v1 backup remains interoperable when platform SHA256 PBKDF2 factory is unavailable`() {
        val password = "বাংলা-password-\uD83D\uDD10".toCharArray()
        val clear = "fixture only".encodeToByteArray()
        val nativeEnvelope = crypto.encrypt(clear, password)
        var fallbackEnvelope: com.hisabnikash.app.backup.model.BackupEnvelope? = null
        // Affect only this test JVM; restore every provider property in finally.
        val originalProviders = Security.getProviders().toList()
        val removed = originalProviders.filter { it.getService("SecretKeyFactory", "PBKDF2WithHmacSHA256") != null }
        val substitutes = removed.mapIndexed { index, provider ->
            object : java.security.Provider("NoPbkdf2Fixture$index", 1.0, "test-only provider fixture") {
                init {
                    putAll(provider.entries.filterNot { (key, value) ->
                        key.toString().contains("PBKDF2WithHmacSHA256", true) || value.toString().contains("PBKDF2WithHmacSHA256", true)
                    }.associate { it.key to it.value })
                }
            }
        }
        try {
            removed.forEach { Security.removeProvider(it.name) }
            substitutes.forEach { Security.addProvider(it) }
            assertTrue(runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }.isFailure)
            assertArrayEquals(clear, crypto.decrypt(nativeEnvelope, password))
            fallbackEnvelope = crypto.encrypt(clear, password)
        } finally {
            substitutes.forEach { Security.removeProvider(it.name) }
            removed.forEach { provider -> Security.insertProviderAt(provider, originalProviders.indexOf(provider) + 1) }
        }
        assertArrayEquals(clear, crypto.decrypt(requireNotNull(fallbackEnvelope), password))
    }
}
