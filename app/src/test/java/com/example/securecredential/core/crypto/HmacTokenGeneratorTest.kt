package com.example.securecredential.core.crypto

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HmacTokenGeneratorTest {

    private fun randomKey(): SecretKey = KeyGenerator.getInstance("HmacSHA256").generateKey()

    @Test
    fun `same key namespace and value produce the same token`() {
        val key = randomKey()
        val tokenA = HmacTokenGenerator.generateToken(key, "domain", "example.com")
        val tokenB = HmacTokenGenerator.generateToken(key, "domain", "example.com")

        assertTrue(tokenA.contentEquals(tokenB))
    }

    @Test
    fun `different namespace produces a different token for the same value (spec 8_2 collision guard)`() {
        val key = randomKey()
        val domainToken = HmacTokenGenerator.generateToken(key, "domain", "example.com")
        val serviceToken = HmacTokenGenerator.generateToken(key, "service", "example.com")

        assertFalse(domainToken.contentEquals(serviceToken))
    }

    @Test
    fun `different value produces a different token`() {
        val key = randomKey()
        val tokenA = HmacTokenGenerator.generateToken(key, "domain", "example.com")
        val tokenB = HmacTokenGenerator.generateToken(key, "domain", "example.org")

        assertFalse(tokenA.contentEquals(tokenB))
    }

    @Test
    fun `different key produces a different token for the same namespace and value`() {
        val tokenA = HmacTokenGenerator.generateToken(randomKey(), "domain", "example.com")
        val tokenB = HmacTokenGenerator.generateToken(randomKey(), "domain", "example.com")

        assertFalse(tokenA.contentEquals(tokenB))
    }
}
