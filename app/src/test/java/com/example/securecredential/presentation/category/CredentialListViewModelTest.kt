package com.example.securecredential.presentation.category

import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.ListCredentialsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CredentialListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty repository yields an empty list, not an error`() = runTest {
        val repo = FakeCredentialRepository().apply { listAllResult = Result.success(emptyList()) }

        val viewModel = CredentialListViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.all.isEmpty())
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loads all stored credentials`() = runTest {
        val summaries = listOf(
            CredentialSummary("id-1", "Google", "google.com", null, null),
            CredentialSummary("id-2", "GitHub", "github.com", null, null)
        )
        val repo = FakeCredentialRepository().apply { listAllResult = Result.success(summaries) }

        val viewModel = CredentialListViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(summaries, viewModel.uiState.value.all)
    }

    @Test
    fun `refresh reloads after a failure`() = runTest {
        val repo = FakeCredentialRepository().apply { listAllResult = Result.failure(RuntimeException("boom")) }
        val viewModel = CredentialListViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("boom", viewModel.uiState.value.errorMessage)

        repo.listAllResult = Result.success(listOf(CredentialSummary("id-1", "Google", "google.com", null, null)))
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals(1, viewModel.uiState.value.all.size)
    }
}
