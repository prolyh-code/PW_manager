package com.example.securecredential.presentation.credential

import androidx.lifecycle.SavedStateHandle
import com.example.securecredential.data.security.CryptoManagerImpl
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.usecase.DeleteCredentialUseCase
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.presentation.authentication.CredentialDisplayState
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CredentialDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun sampleCredential() = Credential(
        credentialId = "id-1", serviceName = "Google", url = null, domain = "google.com",
        username = "alice", password = "hunter2", usernameMask = "a***", passwordMask = "••••",
        category = null, memo = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(repo: FakeCredentialRepository, sessionManager: SessionManagerImpl): CredentialDetailViewModel =
        CredentialDetailViewModel(
            GetCredentialUseCase(repo), DeleteCredentialUseCase(repo), sessionManager,
            SavedStateHandle(mapOf("credentialId" to "id-1"))
        )

    @Test
    fun `starts MASKED and loads the credential`() = runTest {
        val repo = FakeCredentialRepository().apply { getResult = Result.success(sampleCredential()) }
        val sessionManager = SessionManagerImpl(CryptoManagerImpl(), FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

        val viewModel = newViewModel(repo, sessionManager)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CredentialDisplayState.MASKED, viewModel.uiState.value.displayState)
        assertEquals("Google", viewModel.uiState.value.credential?.serviceName)
    }

    @Test
    fun `onViewClicked flips SessionManager to DISPLAYED`() = runTest {
        val repo = FakeCredentialRepository().apply { getResult = Result.success(sampleCredential()) }
        val sessionManager = SessionManagerImpl(CryptoManagerImpl(), FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = newViewModel(repo, sessionManager)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onViewClicked()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CredentialDisplayState.DISPLAYED, viewModel.uiState.value.displayState)
    }

    @Test
    fun `masking via SessionManager clears the held credential from state (spec 9_2)`() = runTest {
        val repo = FakeCredentialRepository().apply { getResult = Result.success(sampleCredential()) }
        val sessionManager = SessionManagerImpl(CryptoManagerImpl(), FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = newViewModel(repo, sessionManager)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onViewClicked()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.credential)

        // Simulates backgrounding (SessionManager.onAppBackgrounded masks immediately, spec 9.1)
        // or an explicit Security Reset — both go through resetCredentialDisplay/onAppBackgrounded.
        sessionManager.resetCredentialDisplay()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(CredentialDisplayState.MASKED, viewModel.uiState.value.displayState)
        assertNull("holding decrypted Credential data after masking violates spec 9.2", viewModel.uiState.value.credential)
    }

    @Test
    fun `onDelete marks the state deleted`() = runTest {
        val repo = FakeCredentialRepository().apply { getResult = Result.success(sampleCredential()) }
        val sessionManager = SessionManagerImpl(CryptoManagerImpl(), FakeAppPreferences(30.seconds), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        val viewModel = newViewModel(repo, sessionManager)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("id-1"), repo.deleted)
        assertEquals(true, viewModel.uiState.value.deleted)
    }
}
