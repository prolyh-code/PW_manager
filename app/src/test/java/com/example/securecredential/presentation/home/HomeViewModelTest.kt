package com.example.securecredential.presentation.home

import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.ListCredentialsUseCase
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

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
    fun `loads recent credentials on init`() = runTest {
        val summary = CredentialSummary("id-1", "Google", "google.com", "a***@x.com", "••••")
        val repo = FakeCredentialRepository().apply { listAllResult = Result.success(listOf(summary)) }

        val viewModel = HomeViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(summary), viewModel.uiState.value.recent)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `query change updates state without triggering a search (search moved to its own screen)`() = runTest {
        val repo = FakeCredentialRepository()
        val viewModel = HomeViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onQueryChange("google.com")

        assertEquals("google.com", viewModel.uiState.value.query)
    }

    @Test
    fun `repository failure surfaces as an error message`() = runTest {
        val repo = FakeCredentialRepository().apply { listAllResult = Result.failure(RuntimeException("db error")) }

        val viewModel = HomeViewModel(ListCredentialsUseCase(repo))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("db error", viewModel.uiState.value.errorMessage)
    }
}
