package com.anchor.app.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keeps the random SQLCipher key wrapped by a non-exportable Android Keystore key. */
object AndroidDatabaseKey {
    private const val alias = "com.anchor.app.database-key"
    private const val preferencesName = "anchor_database_keys"
    private const val version: Byte = 1

    fun getOrCreate(context: Context, databaseName: String): ByteArray {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val preferenceKey = "wrapped_$databaseName"
        preferences.getString(preferenceKey, null)?.let { return unwrap(it) }

        check(!appContext.getDatabasePath(databaseName).exists()) {
            "加密数据库密钥缺失，无法安全打开现有数据库。"
        }

        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, keystoreKey())
        }
        val encrypted = cipher.doFinal(passphrase)
        val packed = byteArrayOf(version, cipher.iv.size.toByte()) + cipher.iv + encrypted
        check(preferences.edit().putString(preferenceKey, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()) {
            "无法保存加密数据库密钥。"
        }
        return passphrase
    }

    private fun unwrap(value: String): ByteArray {
        val packed = Base64.decode(value, Base64.NO_WRAP)
        check(packed.size > 2 && packed[0] == version) { "加密数据库密钥格式无效。" }
        val ivSize = packed[1].toInt() and 0xff
        check(ivSize in 12..16 && packed.size > 2 + ivSize) { "加密数据库密钥格式无效。" }
        val iv = packed.copyOfRange(2, 2 + ivSize)
        val encrypted = packed.copyOfRange(2 + ivSize, packed.size)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, iv))
            doFinal(encrypted)
        }
    }

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }
}
