package com.example.securecredential.core.crypto

import java.nio.CharBuffer
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Fast, salted SHA-256 check used only for immediate "wrong PIN" UX feedback in the app's own
 * keypad (spec 13.3) — NOT the cryptographic security boundary. The Local path is gated by
 * Android Keystore's own authentication requirement; the Recovery path is gated by the
 * PBKDF2-derived KEK's AEAD tag (see [RecoveryKeyManager][com.example.securecredential.data.security.RecoveryKeyManager]).
 * Never used to derive key material.
 */
object PinVerificationHasher {
    private const val SALT_LENGTH_BYTES = 16

    data class Verification(val hash: ByteArray, val salt: ByteArray)

    fun create(pin: CharArray): Verification {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        return Verification(hash(pin, salt), salt)
    }

    fun matches(pin: CharArray, verification: Verification): Boolean =
        MessageDigest.isEqual(hash(pin, verification.salt), verification.hash)

    private fun hash(pin: CharArray, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toUtf8Bytes())
        return digest.digest()
    }

    // Avoids ever materializing a String copy of the PIN (spec invariant #6).
    private fun CharArray.toUtf8Bytes(): ByteArray {
        val byteBuffer = Charsets.UTF_8.encode(CharBuffer.wrap(this))
        val bytes = ByteArray(byteBuffer.remaining())
        byteBuffer.get(bytes)
        return bytes
    }
}
