package com.example.securecredential.core.crypto

import javax.crypto.SecretKey

/**
 * PIN -> Recovery KEK derivation seam (spec 5.2, Open Item #1). MVP ships [Pbkdf2KeyDeriver];
 * swapping in an Argon2id implementation later is a drop-in replacement of this interface.
 */
interface PinKeyDeriver {
    fun deriveKey(pin: CharArray, salt: ByteArray, iterations: Int): SecretKey
}
