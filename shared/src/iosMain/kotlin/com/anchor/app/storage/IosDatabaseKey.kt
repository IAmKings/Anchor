package com.anchor.app.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.noErr
import platform.posix.memcpy

/** Random SQLCipher key stored in the iOS Keychain; not backed up off-device. */
object IosDatabaseKey {
    private const val service = "com.anchor.app.database-key"

    @OptIn(ExperimentalForeignApi::class)
    fun getOrCreate(databaseName: String): ByteArray {
        existing(databaseName)?.let { return it }
        check(!databaseFileExists(databaseName)) {
            "加密数据库密钥缺失，无法安全打开现有数据库。"
        }
        val passphrase = ByteArray(32)
        passphrase.usePinned { pinned ->
            val status = SecRandomCopyBytes(kSecRandomDefault, 32u, pinned.addressOf(0))
            check(status == errSecSuccess || status == noErr.toInt()) { "无法生成数据库密钥。" }
        }
        save(databaseName, passphrase)
        return passphrase
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun existing(databaseName: String): ByteArray? = memScoped {
        val query = queryBase(databaseName).apply {
            put(kSecReturnData, kCFBooleanTrue)
            put(kSecMatchLimit, kSecMatchLimitOne)
        }
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query.asCfDictionary(), result.ptr)
        if (status != errSecSuccess && status != noErr.toInt()) return null
        val cfValue = result.value ?: return null
        val data = interpretObjCPointer<NSData>(cfValue.rawValue)
        ByteArray(data.length.toInt()).also { bytes ->
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun save(databaseName: String, passphrase: ByteArray) {
        val data = passphrase.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = passphrase.size.toULong())
        }
        val query = queryBase(databaseName).apply {
            put(kSecValueData, data)
            put(kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }
        val status = SecItemAdd(query.asCfDictionary(), null)
        check(status == errSecSuccess || status == noErr.toInt()) { "无法保存加密数据库密钥。" }
    }

    private fun databaseFileExists(databaseName: String): Boolean {
        val root = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true,
        ).firstOrNull() as? String ?: return false
        return NSFileManager.defaultManager.fileExistsAtPath("$root/databases/$databaseName")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun queryBase(databaseName: String): NSMutableDictionary {
        return NSMutableDictionary().apply {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrService, service)
            put(kSecAttrAccount, databaseName)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("UNCHECKED_CAST")
private fun NSMutableDictionary.put(key: Any?, value: Any?) {
    setObject(value, forKey = key as NSCopyingProtocol)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSMutableDictionary.asCfDictionary(): CFDictionaryRef =
    interpretCPointer(objcPtr())!!
