package com.example.securecredential.domain.repository

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery

/** Spec section 6.3. Domain <-> Data boundary — UI/UseCase code never sees DAOs or crypto. */
interface CredentialRepository {
    suspend fun create(credential: Credential): Result<String>
    suspend fun update(credential: Credential): Result<Unit>
    suspend fun delete(credentialId: String): Result<Unit>
    suspend fun get(credentialId: String): Result<Credential>
    suspend fun search(query: SearchQuery): Result<List<CredentialSummary>>

    /** All stored credentials as summaries — Home's "Recent"/Category listing (spec 13.3), not a search. */
    suspend fun listAll(): Result<List<CredentialSummary>>

    /** Spec 8.4 — Exact Match only. Selecting a suggestion reuses the username, never the password/mask. */
    suspend fun findByUsername(username: String): Result<List<CredentialSummary>>

    /** Spec section 7. Boolean only — never leaks which/where a password is reused. */
    suspend fun checkPasswordReuse(password: String): Result<Boolean>
}
