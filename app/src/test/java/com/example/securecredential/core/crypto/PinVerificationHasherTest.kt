package com.example.securecredential.core.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinVerificationHasherTest {

    @Test
    fun `correct pin matches its own verification`() {
        val verification = PinVerificationHasher.create("12345678".toCharArray())

        assertTrue(PinVerificationHasher.matches("12345678".toCharArray(), verification))
    }

    @Test
    fun `wrong pin does not match`() {
        val verification = PinVerificationHasher.create("12345678".toCharArray())

        assertFalse(PinVerificationHasher.matches("87654321".toCharArray(), verification))
    }

    @Test
    fun `two verifications of the same pin use different salts and hashes`() {
        val verificationA = PinVerificationHasher.create("12345678".toCharArray())
        val verificationB = PinVerificationHasher.create("12345678".toCharArray())

        assertFalse(verificationA.salt.contentEquals(verificationB.salt))
        assertFalse(verificationA.hash.contentEquals(verificationB.hash))
    }
}
