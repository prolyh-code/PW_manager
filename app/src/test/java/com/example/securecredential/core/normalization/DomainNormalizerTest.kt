package com.example.securecredential.core.normalization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DomainNormalizerTest {

    @Test
    fun `strips https scheme`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com"))
    }

    @Test
    fun `strips http scheme`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("http://example.com"))
    }

    @Test
    fun `strips www prefix`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://www.example.com"))
    }

    @Test
    fun `strips port`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com:8080"))
    }

    @Test
    fun `strips path`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com/login/page"))
    }

    @Test
    fun `strips query string`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com?redirect=/home"))
    }

    @Test
    fun `strips fragment`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com#section"))
    }

    @Test
    fun `strips trailing slash`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("https://example.com/"))
    }

    @Test
    fun `lowercases input`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("HTTPS://EXAMPLE.COM"))
    }

    @Test
    fun `trims whitespace`() {
        assertEquals("example.com", DomainNormalizer.normalizeDomain("  example.com  "))
    }

    @Test
    fun `combined real-world url normalizes correctly`() {
        assertEquals(
            "accounts.google.com",
            DomainNormalizer.normalizeDomain("  HTTPS://WWW.accounts.google.com:443/o/oauth2/auth?client_id=x#frag ")
        )
    }

    @Test
    fun `subdomain is preserved and distinct from the bare domain (spec 8_1)`() {
        val subdomain = DomainNormalizer.normalizeDomain("accounts.google.com")
        val bareDomain = DomainNormalizer.normalizeDomain("google.com")

        assertEquals("accounts.google.com", subdomain)
        assertEquals("google.com", bareDomain)
        assertNotEquals(subdomain, bareDomain)
    }

    @Test
    fun `service name suggestion capitalizes the second-level label`() {
        assertEquals("Google", DomainNormalizer.suggestServiceName("accounts.google.com"))
        assertEquals("Google", DomainNormalizer.suggestServiceName("google.com"))
    }

    @Test
    fun `service name suggestion handles a single-label host`() {
        assertEquals("Localhost", DomainNormalizer.suggestServiceName("localhost"))
    }
}
