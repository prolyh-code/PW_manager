package com.example.securecredential.core.crypto

import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * PBKDF2WithHmacSHA256-based [PinKeyDeriver] (spec 5.2 / Open Item #1 resolution: Argon2id's
 * only maintained Android binding is a stale JNI dependency, not worth the APK-size/build
 * tradeoff for MVP — see plan file risk notes). Built into every API 26+ device, no extra deps.
 */
class Pbkdf2KeyDeriver @Inject constructor() : PinKeyDeriver {

    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        /** OWASP 2023 minimum floor, cited directly in spec 5.2. */
        const val DEFAULT_ITERATIONS = 310_000
        private const val KEY_LENGTH_BITS = 256
        private const val OUTPUT_KEY_ALGORITHM = "AES"
    }

    override fun deriveKey(pin: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(pin, salt, iterations, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM)
            val derived = factory.generateSecret(spec)
            // PBKDF2's SecretKeyFactory output is algorithm-tagged "PBKDF2WithHmacSHA256",
            // not directly usable with an AES Cipher — re-wrap the raw bytes as an AES key.
            SecretKeySpec(derived.encoded, OUTPUT_KEY_ALGORITHM)
        } finally {
            spec.clearPassword()
        }
    }
}
