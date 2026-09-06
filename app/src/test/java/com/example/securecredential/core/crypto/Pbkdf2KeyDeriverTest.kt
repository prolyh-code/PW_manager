package com.example.securecredential.core.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pbkdf2KeyDeriverTest {

    private val deriver = Pbkdf2KeyDeriver()
    // Low iteration count in tests only, so the suite stays fast; production always uses
    // Pbkdf2KeyDeriver.DEFAULT_ITERATIONS (310_000).
    private val testIterations = 1_000

    @Test
    fun `same pin and salt derive the same key deterministically`() {
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val keyA = deriver.deriveKey("MySecurePin123".toCharArray(), salt, testIterations)
        val keyB = deriver.deriveKey("MySecurePin123".toCharArray(), salt, testIterations)

        assertTrue(keyA.encoded.contentEquals(keyB.encoded))
    }

    @Test
    fun `different salt derives a different key for the same pin`() {
        val pin = "MySecurePin123".toCharArray()
        val keyA = deriver.deriveKey(pin, byteArrayOf(1, 2, 3, 4), testIterations)
        val keyB = deriver.deriveKey(pin, byteArrayOf(5, 6, 7, 8), testIterations)

        assertFalse(keyA.encoded.contentEquals(keyB.encoded))
    }

    @Test
    fun `different pin derives a different key for the same salt`() {
        val salt = byteArrayOf(1, 2, 3, 4)
        val keyA = deriver.deriveKey("MySecurePin123".toCharArray(), salt, testIterations)
        val keyB = deriver.deriveKey("AnotherPin456".toCharArray(), salt, testIterations)

        assertFalse(keyA.encoded.contentEquals(keyB.encoded))
    }

    @Test
    fun `derived key is usable as a 256-bit AES key`() {
        val key = deriver.deriveKey("MySecurePin123".toCharArray(), byteArrayOf(1, 2, 3, 4), testIterations)

        assertTrue(key.algorithm == "AES")
        assertTrue(key.encoded.size == 32)
    }
}
