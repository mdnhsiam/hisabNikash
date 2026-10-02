package com.hisabnikash.app.backup.crypto

import com.hisabnikash.app.backup.model.BackupEnvelope
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

class BackupAuthenticationException(cause: Throwable? = null) : Exception("Backup authentication failed", cause)

class BackupCrypto(
    val pbkdf2Iterations: Int = DEFAULT_PBKDF2_ITERATIONS,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    init {
        require(pbkdf2Iterations >= 10_000)
    }

    fun encrypt(snapshotBytes: ByteArray, password: CharArray): BackupEnvelope {
        val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(password, salt, pbkdf2Iterations)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(FORMAT_VERSION, pbkdf2Iterations))
            BackupEnvelope(
                formatVersion = FORMAT_VERSION,
                kdfIterations = pbkdf2Iterations,
                saltBase64 = salt.base64(),
                nonceBase64 = nonce.base64(),
                ciphertextBase64 = cipher.doFinal(snapshotBytes).base64(),
            )
        } finally {
            key.fill(0)
            salt.fill(0)
            nonce.fill(0)
        }
    }

    @Throws(BackupAuthenticationException::class)
    fun decrypt(envelope: BackupEnvelope, password: CharArray): ByteArray {
        val salt: ByteArray
        val nonce: ByteArray
        val ciphertext: ByteArray
        try {
            salt = envelope.saltBase64.fromBase64()
            nonce = envelope.nonceBase64.fromBase64()
            ciphertext = envelope.ciphertextBase64.fromBase64()
        } catch (error: IllegalArgumentException) {
            throw BackupAuthenticationException(error)
        }
        if (salt.size != SALT_BYTES || nonce.size != NONCE_BYTES || envelope.kdfIterations !in 10_000..MAX_PBKDF2_ITERATIONS) {
            salt.fill(0); nonce.fill(0); ciphertext.fill(0)
            throw BackupAuthenticationException()
        }
        val key = deriveKey(password, salt, envelope.kdfIterations)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(aad(envelope.formatVersion, envelope.kdfIterations))
            cipher.doFinal(ciphertext)
        } catch (error: GeneralSecurityException) {
            throw BackupAuthenticationException(error)
        } finally {
            key.fill(0)
            salt.fill(0)
            nonce.fill(0)
            ciphertext.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun aad(version: Int, iterations: Int) = "hisab-backup:$version:$iterations".encodeToByteArray()

    companion object {
        const val FORMAT_VERSION = 1
        const val DEFAULT_PBKDF2_ITERATIONS = 210_000
        private const val MAX_PBKDF2_ITERATIONS = 1_000_000
        private const val SALT_BYTES = 16
        private const val NONCE_BYTES = 12
        private const val KEY_BITS = 256
        private const val TAG_BITS = 128
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

private fun ByteArray.base64(): String = Base64.encode(this)
private fun String.fromBase64(): ByteArray = Base64.decode(this)
