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
 *  on the separate Search Result screen (spec 13.2 nav map: Home -> Search -> Search Result). */
data class HomeUiState(
    val query: String = "",
    val recent: List<CredentialSummary> = emptyList(),
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

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
