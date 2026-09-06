package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import com.example.securecredential.domain.repository.CredentialRepository

/** Fake for pure-JVM UseCase tests — no Room/Keystore involved (those are covered elsewhere). */
class FakeCredentialRepository : CredentialRepository {
    val created = mutableListOf<Credential>()
    val updated = mutableListOf<Credential>()
    val deleted = mutableListOf<String>()
    var searchResult: Result<List<CredentialSummary>> = Result.success(emptyList())
    var listAllResult: Result<List<CredentialSummary>> = Result.success(emptyList())
    var findByUsernameResult: Result<List<CredentialSummary>> = Result.success(emptyList())
    var getResult: Result<Credential>? = null
    var passwordReuseResult: Result<Boolean> = Result.success(false)
    var lastSearchQuery: SearchQuery? = null
    var lastPasswordChecked: String? = null

    override suspend fun create(credential: Credential): Result<String> {
        created.add(credential)
        return Result.success(credential.credentialId.ifBlank { "generated-id" })
    }

    override suspend fun update(credential: Credential): Result<Unit> {
        updated.add(credential)
        return Result.success(Unit)
    }

    override suspend fun delete(credentialId: String): Result<Unit> {
        deleted.add(credentialId)
        return Result.success(Unit)
    }

    override suspend fun get(credentialId: String): Result<Credential> =
        getResult ?: Result.failure(NoSuchElementException(credentialId))

    override suspend fun search(query: SearchQuery): Result<List<CredentialSummary>> {
        lastSearchQuery = query
        return searchResult
    }

    override suspend fun checkPasswordReuse(password: String): Result<Boolean> {
        lastPasswordChecked = password
        return passwordReuseResult
    }

    override suspend fun listAll(): Result<List<CredentialSummary>> = listAllResult

    override suspend fun findByUsername(username: String): Result<List<CredentialSummary>> = findByUsernameResult
}
