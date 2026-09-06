package com.example.securecredential.core.crypto

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Generates fresh, non-Keystore symmetric keys (App Encryption Key, Search HMAC Key — spec 5.1). */
object SymmetricKeyGenerator {
    private const val AES_ALGORITHM = "AES"
    private const val AES_KEY_SIZE_BITS = 256
    private const val HMAC_ALGORITHM = "HmacSHA256"

    fun generateAesKey(): SecretKey =
        KeyGenerator.getInstance(AES_ALGORITHM).apply { init(AES_KEY_SIZE_BITS) }.generateKey()

    fun generateHmacKey(): SecretKey =
        KeyGenerator.getInstance(HMAC_ALGORITHM).generateKey()
}
