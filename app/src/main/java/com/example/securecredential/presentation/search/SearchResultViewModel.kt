package com.example.securecredential.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.model.SearchQuery
import com.example.securecredential.domain.usecase.SearchCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultUiState(
    val query: String = "",
    val isSearching: Boolean = true,
    val results: List<CredentialSummary> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchCredentialsUseCase: SearchCredentialsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchResultUiState())
    val uiState: StateFlow<SearchResultUiState> = _uiState.asStateFlow()

    init {
        val encodedQuery = savedStateHandle.get<String>("query").orEmpty()
        val query = URLDecoder.decode(encodedQuery, "UTF-8")
        _uiState.update { it.copy(query = query) }
        runSearch(query)
    }

    private fun runSearch(query: String) {
        viewModelScope.launch {
            // Exact Search only (spec 8.2/8.3 MVP scope): domain/URL-shaped input searches the
            // domain namespace, otherwise the service-name namespace.
            val looksLikeDomainOrUrl = query.contains('.') || query.contains("://")
            val searchQuery = if (looksLikeDomainOrUrl) SearchQuery.Domain(query) else SearchQuery.Text(query)
            searchCredentialsUseCase(searchQuery).fold(
                onSuccess = { list -> _uiState.update { it.copy(isSearching = false, results = list) } },
                onFailure = { e -> _uiState.update { it.copy(isSearching = false, errorMessage = e.message) } }
            )
        }
    }
}
