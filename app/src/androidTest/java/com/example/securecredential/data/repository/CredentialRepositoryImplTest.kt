package com.example.securecredential.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import com.example.securecredential.data.local.database.CredentialTransactionRunner
import com.example.securecredential.data.local.database.SecureVaultDatabase
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.SearchQuery
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialRepositoryImplTest {

    private lateinit var database: SecureVaultDatabase
    private lateinit var repository: CredentialRepositoryImpl

    private fun sample(serviceName: String = "Google", domain: String? = "accounts.google.com") = Credential(
        credentialId = "",
        serviceName = serviceName,
        url = "https://$domain/login",
        domain = domain,
        username = "alice@example.com",
        password = "hunter2-correct-horse",
        usernameMask = "a***@example.com",
        passwordMask = "********",
        category = "Email",
        memo = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), SecureVaultDatabase::class.java
        ).allowMainThreadQueries().build()

        val cryptoManager = CryptoManagerImpl().apply {
            loadKeys(
                SymmetricKeyGenerator.generateAesKey(),
                SymmetricKeyGenerator.generateHmacKey(),
                SymmetricKeyGenerator.generateHmacKey()
            )
        }
        val transactionRunner = CredentialTransactionRunner(
            database, database.credentialDao(), database.searchIndexDao(), database.usernameIndexDao()
        )
        val passwordReuseChecker = DecryptAllPasswordReuseChecker(database.credentialDao(), cryptoManager)

        repository = CredentialRepositoryImpl(
            database.credentialDao(), database.searchIndexDao(), database.usernameIndexDao(),
            transactionRunner, cryptoManager, passwordReuseChecker
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fullLifecycle_createSearchGetUpdateDelete() = runBlocking {
        val id = repository.create(sample()).getOrThrow()

        val byDomain = repository.search(SearchQuery.Domain("accounts.google.com")).getOrThrow()
        assertEquals(1, byDomain.size)
        assertEquals(id, byDomain[0].credentialId)
        assertEquals("Google", byDomain[0].serviceName)

        val byService = repository.search(SearchQuery.Text("Google")).getOrThrow()
        assertEquals(1, byService.size)

        val fetched = repository.get(id).getOrThrow()
        assertEquals("hunter2-correct-horse", fetched.password)
        assertEquals("alice@example.com", fetched.username)

        repository.update(fetched.copy(memo = "updated memo")).getOrThrow()
        assertEquals("updated memo", repository.get(id).getOrThrow().memo)

        repository.delete(id).getOrThrow()
        assertTrue(repository.get(id).isFailure)
        assertTrue(repository.search(SearchQuery.Domain("accounts.google.com")).getOrThrow().isEmpty())
    }

    @Test
    fun search_withNoMatchingExactToken_returnsEmptyList() = runBlocking {
        repository.create(sample()).getOrThrow()

        val result = repository.search(SearchQuery.Domain("totally-different.example")).getOrThrow()

        assertTrue(result.isEmpty())
    }

    @Test
    fun search_byUrlNormalizesToTheSameDomainToken() = runBlocking {
        repository.create(sample(domain = "accounts.google.com")).getOrThrow()

        val result = repository.search(SearchQuery.Url("https://www.accounts.google.com/some/path?x=1")).getOrThrow()

        assertEquals(1, result.size)
    }

    @Test
    fun subdomainsAreDistinctCredentials_notMergedBySearch() = runBlocking {
        repository.create(sample(serviceName = "Google Accounts", domain = "accounts.google.com")).getOrThrow()
        repository.create(sample(serviceName = "Google", domain = "google.com")).getOrThrow()

        val accountsResult = repository.search(SearchQuery.Domain("accounts.google.com")).getOrThrow()
        val bareResult = repository.search(SearchQuery.Domain("google.com")).getOrThrow()

        assertEquals(1, accountsResult.size)
        assertEquals(1, bareResult.size)
        assertTrue(accountsResult[0].credentialId != bareResult[0].credentialId)
    }
}
