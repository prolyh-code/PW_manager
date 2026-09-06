package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.AesGcmCipher
import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.core.crypto.HmacTokenGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * The only class that holds a decrypted App Encryption Key / Search HMAC Key / Id Index Key
 * for the lifetime of an unlocked session (spec invariant #2/#3, #6). Keys live in memory only
 * while [isUnlocked]; call [clearKeys] on lock/background (spec section 9.1/9.2).
 *
 * Three distinct keys per spec 5.1/6.1: the App Encryption Key never touches search tokens,
 * the Search HMAC Key (domain/service namespaces, spec 8.2) is kept separate from the Id Index
 * Key (username exact-match index, spec 6.1's "HMAC(IdIndexKey, normalizedUsername)") so a
 * compromise of one index's key space doesn't correlate with the other.
 *
 * [useAppEncryptionKey] lets [KeyLifecycleOrchestrator] perform key-rotation operations (e.g.
 * PIN change re-wrapping) without this class ever handing the raw key back to the caller.
 */
interface CryptoManager {
    val isUnlocked: Boolean
    fun loadKeys(appEncryptionKey: SecretKey, searchHmacKey: SecretKey, idIndexKey: SecretKey)
    fun clearKeys()
    fun encryptCredential(plaintext: ByteArray): EncryptedBlob
    fun decryptCredential(blob: EncryptedBlob): ByteArray
    fun searchToken(namespace: String, value: String): ByteArray
    fun usernameToken(normalizedUsername: String): ByteArray
    fun <T> useAppEncryptionKey(block: (SecretKey) -> T): T
}

class CryptoManagerImpl @Inject constructor() : CryptoManager {
    private var appEncryptionKey: SecretKey? = null
    private var searchHmacKey: SecretKey? = null
    private var idIndexKey: SecretKey? = null

    override val isUnlocked: Boolean
        get() = appEncryptionKey != null && searchHmacKey != null && idIndexKey != null

    override fun loadKeys(appEncryptionKey: SecretKey, searchHmacKey: SecretKey, idIndexKey: SecretKey) {
        this.appEncryptionKey = appEncryptionKey
        this.searchHmacKey = searchHmacKey
        this.idIndexKey = idIndexKey
    }

    override fun clearKeys() {
        appEncryptionKey = null
        searchHmacKey = null
        idIndexKey = null
    }

    override fun encryptCredential(plaintext: ByteArray): EncryptedBlob =
        AesGcmCipher.encrypt(requireKey(appEncryptionKey), plaintext)

    override fun decryptCredential(blob: EncryptedBlob): ByteArray =
        AesGcmCipher.decrypt(requireKey(appEncryptionKey), blob)

    override fun searchToken(namespace: String, value: String): ByteArray =
        HmacTokenGenerator.generateToken(requireKey(searchHmacKey), namespace, value)

    override fun usernameToken(normalizedUsername: String): ByteArray =
        HmacTokenGenerator.generateToken(requireKey(idIndexKey), "username", normalizedUsername)

    override fun <T> useAppEncryptionKey(block: (SecretKey) -> T): T =
        block(requireKey(appEncryptionKey))

    private fun requireKey(key: SecretKey?): SecretKey =
        key ?: throw IllegalStateException("CryptoManager is locked: no keys loaded")
}
