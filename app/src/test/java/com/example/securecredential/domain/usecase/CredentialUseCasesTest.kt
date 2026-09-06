package com.example.securecredential.domain.usecase

import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialUseCasesTest {

    private fun sampleCredential(id: String = "id-1") = Credential(
        credentialId = id, serviceName = "Google", url = null, domain = "google.com",
        username = "alice", password = "pw", usernameMask = null, passwordMask = null,
        category = null, memo = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
    )

    @Test
    fun createCredentialUseCase_delegatesToRepository() = runTest {
        val repo = FakeCredentialRepository()
        val result = CreateCredentialUseCase(repo)(sampleCredential())

        assertTrue(result.isSuccess)
        assertEquals(1, repo.created.size)
    }

    @Test
    fun updateCredentialUseCase_delegatesToRepository() = runTest {
        val repo = FakeCredentialRepository()
        UpdateCredentialUseCase(repo)(sampleCredential())

        assertEquals(1, repo.updated.size)
    }

    @Test
    fun deleteCredentialUseCase_delegatesToRepository() = runTest {
        val repo = FakeCredentialRepository()
        DeleteCredentialUseCase(repo)("id-1")

        assertEquals(listOf("id-1"), repo.deleted)
    }

    @Test
    fun getCredentialUseCase_returnsRepositoryResult() = runTest {
        val repo = FakeCredentialRepository().apply { getResult = Result.success(sampleCredential()) }

        val result = GetCredentialUseCase(repo)("id-1")

        assertEquals("id-1", result.getOrThrow().credentialId)
    }

    @Test
    fun searchCredentialsUseCase_passesQueryThroughAndReturnsResult() = runTest {
        val summary = CredentialSummary("id-1", "Google", "google.com", null, null)
        val repo = FakeCredentialRepository().apply { searchResult = Result.success(listOf(summary)) }
        val query = SearchQuery.Domain("google.com")

        val result = SearchCredentialsUseCase(repo)(query)

        assertEquals(query, repo.lastSearchQuery)
        assertEquals(listOf(summary), result.getOrThrow())
    }

    @Test
    fun checkPasswordReuseUseCase_returnsBooleanOnly() = runTest {
        val repo = FakeCredentialRepository().apply { passwordReuseResult = Result.success(true) }

        val result = CheckPasswordReuseUseCase(repo)("some-password")

        assertEquals("some-password", repo.lastPasswordChecked)
        assertTrue(result.getOrThrow())
    }

    @Test
    fun normalizeDomainUseCase_normalizesAndSuggestsServiceName() {
        val result = NormalizeDomainUseCase()("https://WWW.accounts.google.com/login")

        assertEquals("accounts.google.com", result.normalizedDomain)
        assertEquals("Google", result.suggestedServiceName)
    }
}
