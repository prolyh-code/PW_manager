package com.example.securecredential.presentation.credential

import androidx.lifecycle.SavedStateHandle
import com.example.securecredential.domain.usecase.CheckPasswordReuseUseCase
import com.example.securecredential.domain.usecase.CreateCredentialUseCase
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.domain.usecase.NormalizeDomainUseCase
import com.example.securecredential.domain.usecase.SuggestUsernamesUseCase
import com.example.securecredential.domain.usecase.UpdateCredentialUseCase
import com.example.securecredential.presentation.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CredentialFormViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        repo: FakeCredentialRepository,
        credentialId: String = Routes.CredentialForm.NEW
    ) = CredentialFormViewModel(
        CreateCredentialUseCase(repo), UpdateCredentialUseCase(repo), GetCredentialUseCase(repo),
        CheckPasswordReuseUseCase(repo), NormalizeDomainUseCase(), SuggestUsernamesUseCase(repo),
        SavedStateHandle(mapOf("credentialId" to credentialId))
    )

    @Test
    fun `new-credential marker does not trigger a load`() = runTest {
        val repo = FakeCredentialRepository()
        val viewModel = newViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditing)
        assertEquals("", viewModel.uiState.value.serviceName)
    }

    @Test
    fun `entering a url normalizes the domain and suggests a service name`() = runTest {
        val viewModel = newViewModel(FakeCredentialRepository())

        viewModel.onUrlChange("https://WWW.accounts.google.com/login")

        assertEquals("accounts.google.com", viewModel.uiState.value.domain)
        assertEquals("Google", viewModel.uiState.value.serviceName)
    }

    @Test
    fun `manually editing service name stops it from being overwritten by url suggestions`() = runTest {
        val viewModel = newViewModel(FakeCredentialRepository())

        viewModel.onServiceNameChange("My Custom Name")
        viewModel.onUrlChange("https://accounts.google.com/login")

        assertEquals("My Custom Name", viewModel.uiState.value.serviceName)
    }

    @Test
    fun `password reuse check flags a warning`() = runTest {
        val repo = FakeCredentialRepository().apply { passwordReuseResult = Result.success(true) }
        val viewModel = newViewModel(repo)

        viewModel.onPasswordChange("shared-password")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showReuseWarning)
    }

    @Test
    fun `dismissing the reuse warning clears it`() = runTest {
        val repo = FakeCredentialRepository().apply { passwordReuseResult = Result.success(true) }
        val viewModel = newViewModel(repo)
        viewModel.onPasswordChange("shared-password")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissReuseWarning()

        assertFalse(viewModel.uiState.value.showReuseWarning)
    }

    @Test
    fun `save on a new credential calls create, not update`() = runTest {
        val repo = FakeCredentialRepository()
        val viewModel = newViewModel(repo)
        viewModel.onServiceNameChange("Google")
        viewModel.onUsernameChange("alice")
        viewModel.onPasswordChange("pw")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.created.size)
        assertTrue(repo.updated.isEmpty())
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `save on an existing credential calls update, not create`() = runTest {
        val repo = FakeCredentialRepository().apply {
            getResult = Result.success(
                com.example.securecredential.domain.model.Credential(
                    "id-1", "Google", null, "google.com", "alice", "pw", null, null, null, null,
                    java.time.Instant.EPOCH, java.time.Instant.EPOCH
                )
            )
        }
        val viewModel = newViewModel(repo, credentialId = "id-1")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repo.updated.size)
        assertTrue(repo.created.isEmpty())
        assertEquals("id-1", repo.updated.first().credentialId)
    }
}
