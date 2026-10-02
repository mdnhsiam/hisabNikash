package com.hisabnikash.app.backup.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import androidx.core.content.edit

interface BackupPasswordCache {
    fun save(password: CharArray)
    fun load(): CharArray?
    fun clear()
}

class AndroidBackupSecretStore(context: Context) : BackupPasswordCache {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun save(password: CharArray) {
        val encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password))
        val bytes = ByteArray(encoded.remaining()).also(encoded::get)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            preferences.edit {
                putString(KEY_CIPHERTEXT, Base64.encodeToString(cipher.doFinal(bytes), Base64.NO_WRAP))
                putString(KEY_NONCE, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            }
        } finally {
            bytes.fill(0)
        }
    }

    override fun load(): CharArray? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val nonce = preferences.getString(KEY_NONCE, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        val clear = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, nonce))
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            clear()
            return null
        } finally {
            ciphertext.fill(0)
            nonce.fill(0)
        }
        return try {
            StandardCharsets.UTF_8.decode(ByteBuffer.wrap(clear)).let { chars -> CharArray(chars.remaining()).also(chars::get) }
        } finally {
            clear.fill(0)
        }
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "hisab_backup_secret"
        private const val KEY_ALIAS = "hisab_backup_password_key"
        private const val KEY_CIPHERTEXT = "ciphertext"
        private const val KEY_NONCE = "nonce"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
