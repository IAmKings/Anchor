@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.anchor.app.storage

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import com.anchor.app.db.AnchorDatabase
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask

const val IOS_DATABASE_NAME = "anchor.db"

class IosEncryptedProbeStore private constructor(
    private val databaseName: String,
    private val driver: NativeSqliteDriver,
    private val impl: SqlDelightAnchorStore,
) : AnchorStore by impl, AutoCloseable {
    constructor(databaseName: String, passphrase: ByteArray) : this(
        databaseName = databaseName,
        driver = createEncryptedDriver(databaseName, passphrase),
    )

    private constructor(databaseName: String, driver: NativeSqliteDriver) : this(
        databaseName = databaseName,
        driver = driver,
        impl = SqlDelightAnchorStore(
            queries = AnchorDatabase(driver).encryptedProbeQueries,
            deleteAudioFile = { name ->
                val path = "${voiceNotesDir()}/$name"
                val manager = NSFileManager.defaultManager
                check(!manager.fileExistsAtPath(path) || manager.removeItemAtPath(path, null)) {
                    "无法删除关联录音。"
                }
            },
            clearLocalFiles = {
                val path = voiceNotesDir()
                val manager = NSFileManager.defaultManager
                check(!manager.fileExistsAtPath(path) || manager.removeItemAtPath(path, null)) {
                    "无法删除本地录音。"
                }
            },
        ),
    )

    fun write(value: String) = impl.write(value)

    fun read(): String? = impl.read()

    override fun close() {
        driver.close()
    }

    fun delete(): Boolean {
        close()
        val path = databasePath(databaseName)
        val manager = NSFileManager.defaultManager
        return !manager.fileExistsAtPath(path) || manager.removeItemAtPath(path, null)
    }
}

private fun createEncryptedDriver(databaseName: String, passphrase: ByteArray): NativeSqliteDriver {
    val key = passphrase.toSqlCipherKey()
    val driver = NativeSqliteDriver(
        schema = AnchorDatabase.Schema,
        name = databaseName,
        onConfiguration = { config ->
            config.copy(
                encryptionConfig = DatabaseConfiguration.Encryption(key = key),
            )
        },
    )
    val version = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA cipher_version",
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null)
        },
        parameters = 0,
    ).value
    check(!version.isNullOrBlank()) {
        "iOS 需要链接 SQLCipher。当前打开的不是加密库。"
    }
    return driver
}

private fun voiceNotesDir(): String = "${supportRoot()}/voice-notes"

private fun databasePath(databaseName: String): String = "${supportRoot()}/databases/$databaseName"

private fun supportRoot(): String =
    NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true,
    ).first() as String
