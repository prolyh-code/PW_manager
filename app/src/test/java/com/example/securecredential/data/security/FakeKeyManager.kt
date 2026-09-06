package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.AesGcmCipher
import com.example.securecredential.core.crypto.EncryptedBlob
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * In-memory [KeyManager] standing in for a real "AndroidKeyStore" provider (unavailable on the
 * plain JVM test runner). Used only to unit-test [KeyLifecycleOrchestrator]'s sequencing and
 * state transitions — the real Keystore-backed path is verified separately by
 * AndroidKeystoreKeyManagerTest (androidTest).
 */
class FakeKeyManager : KeyManager {
    private var deviceMasterKey: SecretKey? = null

    override fun deviceMasterKeyExists(): Boolean = deviceMasterKey != null

    override fun generateDeviceMasterKey(requireUserAuthentication: Boolean) {
        deviceMasterKey = SymmetricKeyGenerator.generateAesKey()
    }

    override fun wrapLocal(appEncryptionKey: SecretKey): EncryptedBlob =
        AesGcmCipher.encrypt(requireKey(), appEncryptionKey.encoded)

    override fun unwrapLocal(wrapped: EncryptedBlob): SecretKey =
        javax.crypto.spec.SecretKeySpec(AesGcmCipher.decrypt(requireKey(), wrapped), "AES")

    override fun createDecryptCipher(wrapped: EncryptedBlob): Cipher =
        throw UnsupportedOperationException("FakeKeyManager does not simulate the CryptoObject flow")

    override fun finishUnwrapLocal(cipher: Cipher, wrapped: EncryptedBlob): SecretKey =
        throw UnsupportedOperationException("FakeKeyManager does not simulate the CryptoObject flow")

    override fun regenerateDeviceMasterKeyAndRewrap(
        appEncryptionKey: SecretKey,
        requireUserAuthentication: Boolean
    ): EncryptedBlob {
        deleteDeviceMasterKey()
        generateDeviceMasterKey(requireUserAuthentication)
        return wrapLocal(appEncryptionKey)
    }

    override fun deleteDeviceMasterKey() {
        deviceMasterKey = null
    }

    private fun requireKey(): SecretKey = deviceMasterKey ?: error("Device Master Key not generated yet")
}
