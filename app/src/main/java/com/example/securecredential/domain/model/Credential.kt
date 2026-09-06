package com.example.securecredential.domain.model

import java.time.Instant

/** Spec section 4. Full plaintext form — only decrypted when a specific item is opened. */
data class Credential(
    val credentialId: String,
    val serviceName: String,
    val url: String?,
    val domain: String?,
    val username: String,
    val password: String,
    val usernameMask: String?,
    val passwordMask: String?,
    val category: String?,
    val memo: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/** Spec section 4. Search-result-only projection — never carries a plaintext password. */
data class CredentialSummary(
    val credentialId: String,
    val serviceName: String,
    val domain: String?,
    val usernameMask: String?,
    val passwordMask: String?
)
