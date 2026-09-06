package com.example.securecredential.presentation.backup

import com.example.securecredential.core.crypto.Pbkdf2KeyDeriver
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.data.security.FakeKeyManager
import com.example.securecredential.data.security.InMemoryKeyMaterialStore
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import com.example.securecredential.data.security.RecoveryKeyManager
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.domain.usecase.ListCredentialsUseCase
import com.example.securecredential.presentation.authentication.AppSessionState
import com.example.securecredential.presentation.authentication.FakeAppPreferences
import com.example.securecredential.presentation.authentication.SessionManagerImpl
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestoreViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Fixture(
        val repo: FakeCredentialRepository = FakeCredentialRepository()
    ) {
        val cryptoManager = CryptoManagerImpl()
        val orchestrator = KeyLifecycleOrchestrator(
            FakeKeyManager(), RecoveryKeyManager(Pbkdf2KeyDeriver()), cryptoManager, InMemoryKeyMaterialStore()
        )
        val sessionManager = SessionManagerImpl(cryptoManager, FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = BackupRestoreViewModel(
            orchestrator, cryptoManager, ListCredentialsUseCase(repo), GetCredentialUseCase(repo), sessionManager
        )
    }

    @Test
    fun `wrong pin never reaches the decrypt-test or unlocks the session`() = runTest {
        val fixture = Fixture()
        fixture.orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        fixture.cryptoManager.clearKeys() // simulate "new device": nothing loaded yet

        "99998888".forEach { fixture.viewModel.appendPinDigit(it) }
        fixture.viewModel.submitRestorePin()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(fixture.viewModel.uiState.value.errorMessage)
        assertFalse(fixture.viewModel.uiState.value.restoreComplete)
        assertFalse(fixture.cryptoManager.isUnlocked)
        assertEquals(AppSessionState.LOCKED, fixture.sessionManager.appState.value)
    }

    @Test
    fun `empty backup has nothing to decrypt-test against and still completes`() = runTest {
        val fixture = Fixture()
        fixture.orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        fixture.cryptoManager.clearKeys()
        fixture.repo.listAllResult = Result.success(emptyList())

        "11112222".forEach { fixture.viewModel.appendPinDigit(it) }
        fixture.viewModel.submitRestorePin()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.restoreComplete)
        assertTrue(fixture.cryptoManager.isUnlocked)
        assertEquals(AppSessionState.UNLOCKED, fixture.sessionManager.appState.value)
    }

    @Test
    fun `a failed decrypt-test clears keys and does not complete the restore (AC-BACKUP-07)`() = runTest {
        val fixture = Fixture()
        fixture.orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        fixture.cryptoManager.clearKeys()
        fixture.repo.listAllResult = Result.success(listOf(CredentialSummary("id-1", "Google", "google.com", null, null)))
        fixture.repo.getResult = Result.failure(RuntimeException("simulated corruption")) // decrypt-test fails

        "11112222".forEach { fixture.viewModel.appendPinDigit(it) }
        fixture.viewModel.submitRestorePin()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(fixture.viewModel.uiState.value.restoreComplete)
        assertNotNull(fixture.viewModel.uiState.value.errorMessage)
        assertFalse("a failed decrypt-test must not leave keys loaded", fixture.cryptoManager.isUnlocked)
    }

    @Test
    fun `a successful decrypt-test completes the restore and unlocks the session`() = runTest {
        val fixture = Fixture()
        fixture.orchestrator.onFirstLaunch("11112222".toCharArray(), requireBiometric = false)
        fixture.cryptoManager.clearKeys()
        fixture.repo.listAllResult = Result.success(listOf(CredentialSummary("id-1", "Google", "google.com", null, null)))
        fixture.repo.getResult = Result.success(
            Credential("id-1", "Google", null, "google.com", "alice", "pw", null, null, null, null, Instant.EPOCH, Instant.EPOCH)
        )

        "11112222".forEach { fixture.viewModel.appendPinDigit(it) }
        fixture.viewModel.submitRestorePin()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(fixture.viewModel.uiState.value.restoreComplete)
        assertTrue(fixture.cryptoManager.isUnlocked)
    }
}
