package com.anchor.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class AndroidEncryptedExport(
    private val context: Context,
    private val cacheFolder: String = CACHE_FOLDER,
) {
    fun create(password: CharArray, json: String, csv: String): File {
        require(password.size >= 8) { "密码至少需要 8 个字符" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val plaintext = zip(json, csv)
        val encrypted = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(MAGIC)
            doFinal(plaintext)
        }
        plaintext.fill(0)

        val directory = File(context.cacheDir, cacheFolder).apply { mkdirs() }
        directory.listFiles()?.forEach(File::delete)
        return File(directory, "anchor-${timestamp()}.anchor").also { file ->
            DataOutputStream(file.outputStream()).use {
                it.write(MAGIC)
                it.writeInt(ITERATIONS)
                it.write(salt)
                it.write(nonce)
                it.write(encrypted)
            }
        }
    }

    fun shareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    internal fun decrypt(file: File, password: CharArray): Map<String, String> {
        val input = DataInputStream(file.inputStream())
        val magic = ByteArray(MAGIC.size).also(input::readFully)
        require(magic.contentEquals(MAGIC)) { "不是 Anchor 导出文件" }
        val iterations = input.readInt()
        require(iterations in 1..ITERATIONS) { "不支持的加密参数" }
        val salt = ByteArray(SALT_BYTES).also(input::readFully)
        val nonce = ByteArray(NONCE_BYTES).also(input::readFully)
        val encrypted = input.readBytes()
        input.close()
        val plaintext = Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(MAGIC)
            doFinal(encrypted)
        }
        return unzip(plaintext).also { plaintext.fill(0) }
    }

    private fun zip(json: String, csv: String) = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { zip ->
            mapOf("anchor.json" to json, "anchor.csv" to csv).forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        bytes.toByteArray()
    }

    private fun unzip(bytes: ByteArray): Map<String, String> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                require(entry.name == "anchor.json" || entry.name == "anchor.csv")
                put(entry.name, zip.readBytes().decodeToString())
                entry = zip.nextEntry
            }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun timestamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

    companion object {
        const val CACHE_FOLDER = "exports"
        const val INSTRUMENTED_CACHE_FOLDER = "exports-instrumented"
        private val MAGIC = "ANCHOR01".toByteArray()
        private val random = SecureRandom()
        private const val ITERATIONS = 120_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val MIME_TYPE = "application/vnd.anchor.encrypted-export"
    }
}
