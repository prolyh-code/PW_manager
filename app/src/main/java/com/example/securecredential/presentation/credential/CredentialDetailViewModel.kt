package com.example.securecredential.presentation.credential

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.usecase.DeleteCredentialUseCase
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.presentation.authentication.CredentialDisplayState
import com.example.securecredential.presentation.authentication.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Spec 9.2's CredentialDetailUiState shape (credential/displayState/isLoading/error). */
data class CredentialDetailUiState(
    val credential: Credential? = null,
    val displayState: CredentialDisplayState = CredentialDisplayState.MASKED,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class CredentialDetailViewModel @Inject constructor(
    private val getCredentialUseCase: GetCredentialUseCase,
    private val deleteCredentialUseCase: DeleteCredentialUseCase,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val credentialId: String = checkNotNull(savedStateHandle["credentialId"])

    private val _uiState = MutableStateFlow(CredentialDetailUiState())
    val uiState: StateFlow<CredentialDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        // Spec 9.2: never hold decrypted Credential data once the app masks it (background or
        // Security Reset) — react to SessionManager rather than duplicating that state machine.
        // drop(1): credentialDisplayState is a StateFlow that immediately replays its current
        // value (MASKED, the baseline) on collection — without dropping that replay, this would
        // wipe out the credential load() just performed even though nothing actually happened.
        sessionManager.credentialDisplayState.drop(1).onEach { displayState ->
            _uiState.update { state ->
                if (displayState == CredentialDisplayState.MASKED) {
                    state.copy(displayState = displayState, credential = null)
                } else {
                    state.copy(displayState = displayState)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getCredentialUseCase(credentialId).fold(
                onSuccess = { credential -> _uiState.update { it.copy(isLoading = false, credential = credential) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
            )
        }
    }

    fun onViewClicked() {
        sessionManager.displayCredential()
        if (_uiState.value.credential == null) load()
    }

    /** Security Reset (spec 9.1/11) — masks the display only, never touches stored data. */
    fun onSecurityReset() {
        sessionManager.resetCredentialDisplay()
    }

    fun onDelete() {
        viewModelScope.launch {
            deleteCredentialUseCase(credentialId).fold(
                onSuccess = { _uiState.update { it.copy(deleted = true) } },
                onFailure = { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            )
        }
    }
}
