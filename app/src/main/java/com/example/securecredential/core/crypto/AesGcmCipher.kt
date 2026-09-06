package com.example.securecredential.core.crypto

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ciphertext/nonce pair. Note: as a data class over ByteArray, == and hashCode are
 * reference-based, not content-based — use contentEquals for value comparisons (e.g. in tests).
 */
data class EncryptedBlob(val ciphertext: ByteArray, val nonce: ByteArray)

/** Thrown when GCM tag verification fails — tampered ciphertext or (for KEK-wrapped data) a wrong key. */
class AeadIntegrityException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Pure AES-256-GCM wrapper (spec section 1, 5.1). Knows nothing about Keystore, Room, or
 * app-level key roles — safe to unit test with any in-memory SecretKey.
 */
object AesGcmCipher {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val NONCE_LENGTH_BYTES = 12

    private val secureRandom = SecureRandom()

    fun encrypt(key: SecretKey, plaintext: ByteArray): EncryptedBlob {
        val nonce = ByteArray(NONCE_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedBlob(ciphertext, nonce)
    }

    fun decrypt(key: SecretKey, blob: EncryptedBlob): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, blob.nonce))
        return try {
            cipher.doFinal(blob.ciphertext)
        } catch (e: AEADBadTagException) {
            throw AeadIntegrityException("Ciphertext failed integrity/authenticity check", e)
        }
    }
}
