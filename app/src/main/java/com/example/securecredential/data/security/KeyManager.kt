package com.example.securecredential.data.security

import com.example.securecredential.core.crypto.EncryptedBlob
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Owns the Device Master Key (Android Keystore, non-exportable, spec 5.1) and the Local
 * Wrapped Key path only. Never touches the Recovery path — see [RecoveryKeyManager].
 *
 * [createDecryptCipher]/[finishUnwrapLocal] are split in two so a biometric-gated key's
 * Cipher can be handed to BiometricPrompt as a CryptoObject *before* authentication, then
 * finished after success (spec 5.3). [unwrapLocal] is the non-gated shortcut, valid when the
 * Device Master Key was generated with `requireUserAuthentication = false`.
 */
interface KeyManager {
    fun deviceMasterKeyExists(): Boolean

    /**
     * When [requireUserAuthentication] is true, the key is generated with
     * `setInvalidatedByBiometricEnrollment(true)` (deliberate — see plan file risk #1) and
     * requires a BiometricPrompt-authenticated CryptoObject for every decrypt.
     */
    fun generateDeviceMasterKey(requireUserAuthentication: Boolean)

    fun wrapLocal(appEncryptionKey: SecretKey): EncryptedBlob

    /** Only valid for a key generated with `requireUserAuthentication = false`. */
    fun unwrapLocal(wrapped: EncryptedBlob): SecretKey

    /** Cipher to hand to BiometricPrompt as part of a CryptoObject, pre-authentication. */
    fun createDecryptCipher(wrapped: EncryptedBlob): Cipher

    /** Call with the same (now-authenticated) Cipher after BiometricPrompt succeeds. */
    fun finishUnwrapLocal(cipher: Cipher, wrapped: EncryptedBlob): SecretKey

    /** Used post-Restore (spec 5.4): new device, so a fresh Device Master Key is required. */
    fun regenerateDeviceMasterKeyAndRewrap(
        appEncryptionKey: SecretKey,
        requireUserAuthentication: Boolean
    ): EncryptedBlob

    fun deleteDeviceMasterKey()
}
