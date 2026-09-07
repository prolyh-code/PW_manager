package com.example.securecredential.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.domain.usecase.ListCredentialsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Spec 9.2's HomeUiState shape (query/results/error) minus the results list, which now lives
 *  on the separate Search Result screen (spec 13.2 nav map: Home -> Search -> Search Result).
 *  [suggestions] is a separate, later addition — see the note on [HomeViewModel.onQueryChange]. */
data class HomeUiState(
    val query: String = "",
    val recent: List<CredentialSummary> = emptyList(),
    val suggestions: List<CredentialSummary> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listCredentialsUseCase: ListCredentialsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRecent()
    }

    fun loadRecent() {
        viewModelScope.launch {
            listCredentialsUseCase().fold(
                onSuccess = { list -> _uiState.update { it.copy(recent = list) } },
                onFailure = { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            )
        }
    }

    /**
     * Live per-character autocomplete, added at the user's explicit request as a deliberate
     * deviation from spec 8.3/8.4 (which scopes only Exact Search into the MVP and defers any
     * partial-match capability to P1 pending its own security review).
     *
     * That spec concern is about a *database-level* index capable of partial matches while data
     * stays encrypted at rest — HMAC tokens can't support prefix queries, and building an index
     * that could (e.g. N-grams) would leak more about stored values than the current design
     * does. This is a different, narrower thing: a client-side `startsWith` filter over
     * [HomeUiState.recent], which is already the *fully decrypted* credential list this screen
     * holds in memory for its "Recent" section during an UNLOCKED session (spec invariant #2
     * about ciphertext-at-rest is untouched — nothing new is written to disk, no new index
     * exists, and CryptoManager's held keys/decrypted state behave exactly as before; this is
     * pure in-memory list filtering, the same operation the "Recent" section already performs
     * by showing that same list unfiltered).
     */
    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val suggestions = if (query.isBlank()) {
                emptyList()
            } else {
                state.recent.filter { summary ->
                    summary.domain?.startsWith(query, ignoreCase = true) == true ||
                        summary.serviceName.startsWith(query, ignoreCase = true)
                }
            }
            state.copy(query = query, suggestions = suggestions)
        }
    }
}
