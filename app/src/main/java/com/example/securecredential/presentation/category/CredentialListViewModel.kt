package com.example.securecredential.presentation.category

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

data class CredentialListUiState(
    val isLoading: Boolean = true,
    val all: List<CredentialSummary> = emptyList(),
    val errorMessage: String? = null
)

/** Spec 13.2 "Category -> Credential List -> Credential Detail". MVP shows the full list — a
 *  real category taxonomy/filter is not specified anywhere in the spec, so grouping by the
 *  user-authored `category` field would need product input; deferred until asked for. */
@HiltViewModel
class CredentialListViewModel @Inject constructor(
    private val listCredentialsUseCase: ListCredentialsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialListUiState())
    val uiState: StateFlow<CredentialListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            listCredentialsUseCase().fold(
                onSuccess = { list -> _uiState.update { it.copy(isLoading = false, all = list) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            )
        }
    }
}
