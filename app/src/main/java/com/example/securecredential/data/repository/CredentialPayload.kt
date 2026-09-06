package com.example.securecredential.data.repository

import kotlinx.serialization.Serializable

/**
 * Plaintext shape serialized (kotlinx.serialization JSON) then AES-GCM encrypted into
 * CredentialEntity.encryptedPayload (spec 6.1). credentialId/createdAt/updatedAt live as plain
 * CredentialEntity columns instead — they aren't sensitive and staying queryable is harmless.
 */
@Serializable
data class CredentialPayload(
    val serviceName: String,
    val url: String?,
    val domain: String?,
    val username: String,
    val password: String,
    val usernameMask: String?,
    val passwordMask: String?,
    val category: String?,
    val memo: String?
)
