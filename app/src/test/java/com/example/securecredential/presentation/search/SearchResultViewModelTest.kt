package com.example.securecredential.presentation.search

import androidx.lifecycle.SavedStateHandle
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import com.example.securecredential.domain.usecase.FakeCredentialRepository
import com.example.securecredential.domain.usecase.SearchCredentialsUseCase
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

class SearchResultViewModelTest {

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
    fun `domain-shaped query searches the domain namespace and surfaces results`() = runTest {
        val summary = CredentialSummary("id-1", "Google", "google.com", "a***@x.com", "••••")
        val repo = FakeCredentialRepository().apply { searchResult = Result.success(listOf(summary)) }

        val viewModel = SearchResultViewModel(
            SearchCredentialsUseCase(repo), SavedStateHandle(mapOf("query" to "google.com"))
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.lastSearchQuery is SearchQuery.Domain)
        assertEquals(listOf(summary), viewModel.uiState.value.results)
        assertEquals(false, viewModel.uiState.value.isSearching)
    }

    @Test
    fun `plain text query searches the service namespace`() = runTest {
        val repo = FakeCredentialRepository()

        SearchResultViewModel(SearchCredentialsUseCase(repo), SavedStateHandle(mapOf("query" to "Google")))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.lastSearchQuery is SearchQuery.Text)
    }

    @Test
    fun `no exact match yields an empty results list, not an error`() = runTest {
        val repo = FakeCredentialRepository().apply { searchResult = Result.success(emptyList()) }

        val viewModel = SearchResultViewModel(SearchCredentialsUseCase(repo), SavedStateHandle(mapOf("query" to "nope.com")))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.results.isEmpty())
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }
}
