package com.example.securecredential.core.crypto

import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Search token generator (spec section 8.2). Namespace prefixes ("domain", "service", ...)
 * are mandatory so the two search spaces never collide on the same HMAC token.
 */
object HmacTokenGenerator {
    private const val ALGORITHM = "HmacSHA256"

    fun generateToken(key: SecretKey, namespace: String, value: String): ByteArray {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(key)
        return mac.doFinal("$namespace:$value".toByteArray(Charsets.UTF_8))
    }
}
