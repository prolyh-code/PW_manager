package com.example.securecredential.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.securecredential.core.crypto.EncryptedBlob
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * [KeyManager] backed by the real "AndroidKeyStore" provider. Requires an Android runtime —
 * exercised by instrumented tests (androidTest), not plain unit tests (spec: Robolectric
 * cannot faithfully emulate hardware-backed Keystore behavior).
 */
class AndroidKeystoreKeyManager : KeyManager {

    companion object {
        private const val PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "device_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val OUTPUT_KEY_ALGORITHM = "AES"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

    override fun deviceMasterKeyExists(): Boolean = keyStore.containsAlias(ALIAS)

    override fun generateDeviceMasterKey(requireUserAuthentication: Boolean) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        val specBuilder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            // Deliberate (plan file risk #1): a new biometric enrollment permanently
            // invalidates this key. Callers must catch KeyPermanentlyInvalidatedException
            // on unwrap and fall back to the Recovery/PIN path.
            .setInvalidatedByBiometricEnrollment(true)

        if (requireUserAuthentication) {
            specBuilder
                .setUserAuthenticationRequired(true)
                // -1 = require authentication for every single use (the BiometricPrompt
                // CryptoObject flow), rather than a time-bound unlock window.
                .setUserAuthenticationValidityDurationSeconds(-1)
        }

        keyGenerator.init(specBuilder.build())
        keyGenerator.generateKey()
    }

    override fun wrapLocal(appEncryptionKey: SecretKey): EncryptedBlob {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deviceMasterKey())
        val ciphertext = cipher.doFinal(appEncryptionKey.encoded)
        return EncryptedBlob(ciphertext, cipher.iv)
    }

    override fun unwrapLocal(wrapped: EncryptedBlob): SecretKey =
        finishUnwrapLocal(createDecryptCipher(wrapped), wrapped)

    override fun createDecryptCipher(wrapped: EncryptedBlob): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            deviceMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, wrapped.nonce)
        )
        return cipher
    }

    override fun finishUnwrapLocal(cipher: Cipher, wrapped: EncryptedBlob): SecretKey {
        val rawKey = cipher.doFinal(wrapped.ciphertext)
        return SecretKeySpec(rawKey, OUTPUT_KEY_ALGORITHM)
    }

    override fun regenerateDeviceMasterKeyAndRewrap(
        appEncryptionKey: SecretKey,
        requireUserAuthentication: Boolean
    ): EncryptedBlob {
        deleteDeviceMasterKey()
        generateDeviceMasterKey(requireUserAuthentication)
        return wrapLocal(appEncryptionKey)
    }

    override fun deleteDeviceMasterKey() {
        if (keyStore.containsAlias(ALIAS)) {
            keyStore.deleteEntry(ALIAS)
        }
    }

    private fun deviceMasterKey(): SecretKey = keyStore.getKey(ALIAS, null) as SecretKey
}
