package com.example.securecredential.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.securecredential.core.crypto.SymmetricKeyGenerator
import com.example.securecredential.data.local.database.CredentialTransactionRunner
import com.example.securecredential.data.local.database.SecureVaultDatabase
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.domain.model.Credential
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DecryptAllPasswordReuseCheckerTest {

    private lateinit var database: SecureVaultDatabase
    private lateinit var repository: CredentialRepositoryImpl
    private lateinit var checker: DecryptAllPasswordReuseChecker

    private fun credential(serviceName: String, password: String) = Credential(
        credentialId = "", serviceName = serviceName, url = null, domain = null,
        username = "user-$serviceName", password = password, usernameMask = null, passwordMask = null,
        category = null, memo = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
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
        checker = DecryptAllPasswordReuseChecker(database.credentialDao(), cryptoManager)
        repository = CredentialRepositoryImpl(
            database.credentialDao(), database.searchIndexDao(), database.usernameIndexDao(),
            transactionRunner, cryptoManager, checker
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun isReused_falseWhenTableEmpty() = runBlocking {
        assertFalse(checker.isReused("anything"))
    }

    @Test
    fun isReused_trueWhenAnExistingCredentialHasTheSamePassword() = runBlocking {
        repository.create(credential("ServiceA", "shared-password-123")).getOrThrow()
        repository.create(credential("ServiceB", "different-password-456")).getOrThrow()

        assertTrue(checker.isReused("shared-password-123"))
    }

    @Test
    fun isReused_falseWhenNoCredentialMatches() = runBlocking {
        repository.create(credential("ServiceA", "shared-password-123")).getOrThrow()

        assertFalse(checker.isReused("not-used-anywhere"))
    }

    @Test
    fun repositoryCheckPasswordReuse_onlyExposesABoolean() = runBlocking {
        repository.create(credential("ServiceA", "shared-password-123")).getOrThrow()

        val result = repository.checkPasswordReuse("shared-password-123")

        // Domain boundary: Result<Boolean> only, per spec 7 — no way for a caller to learn
        // which credential matched even by inspecting the return type.
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow())
    }
}
