package com.example.securecredential.presentation.credential

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.R
import com.example.securecredential.core.util.UiText
import com.example.securecredential.domain.model.Credential
import com.example.securecredential.domain.usecase.CheckPasswordReuseUseCase
import com.example.securecredential.domain.usecase.CreateCredentialUseCase
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.domain.usecase.NormalizeDomainUseCase
import com.example.securecredential.domain.usecase.SuggestUsernamesUseCase
import com.example.securecredential.domain.usecase.UpdateCredentialUseCase
import com.example.securecredential.domain.model.CredentialSummary
import com.example.securecredential.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Spec 13.3 Credential Registration/Edit — Service/URL/ID/Password/Mask(user-authored)/Category/Memo. */
data class CredentialFormUiState(
    val credentialId: String? = null, // null while creating
    val serviceName: String = "",
    val url: String = "",
    val domain: String = "",
    val username: String = "",
    val password: String = "",
    val usernameMask: String = "",
    val passwordMask: String = "",
    val category: String = "",
    val memo: String = "",
    val serviceNameEditedByUser: Boolean = false,
    val showReuseWarning: Boolean = false,
    val usernameSuggestions: List<CredentialSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: UiText? = null,
    val saved: Boolean = false
) {
    val isEditing: Boolean get() = credentialId != null
}

@HiltViewModel
class CredentialFormViewModel @Inject constructor(
    private val createCredentialUseCase: CreateCredentialUseCase,
    private val updateCredentialUseCase: UpdateCredentialUseCase,
    private val getCredentialUseCase: GetCredentialUseCase,
    private val checkPasswordReuseUseCase: CheckPasswordReuseUseCase,
    private val normalizeDomainUseCase: NormalizeDomainUseCase,
    private val suggestUsernamesUseCase: SuggestUsernamesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredentialFormUiState())
    val uiState: StateFlow<CredentialFormUiState> = _uiState.asStateFlow()

    init {
        val existingId = savedStateHandle.get<String>("credentialId")
        if (!existingId.isNullOrBlank() && existingId != Routes.CredentialForm.NEW) {
            loadExisting(existingId)
        }
    }

    private fun loadExisting(id: String) {
        _uiState.update { it.copy(isLoading = true, credentialId = id) }
        viewModelScope.launch {
            getCredentialUseCase(id).fold(
                onSuccess = { c ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            serviceName = c.serviceName, url = c.url.orEmpty(), domain = c.domain.orEmpty(),
                            username = c.username, password = c.password,
                            usernameMask = c.usernameMask.orEmpty(), passwordMask = c.passwordMask.orEmpty(),
                            category = c.category.orEmpty(), memo = c.memo.orEmpty(),
                            serviceNameEditedByUser = true
                        )
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message?.let { m -> UiText.Dynamic(m) }) } }
            )
        }
    }

    /** Spec 8.1: live normalize + suggest a Service Name the user can still override. */
    fun onUrlChange(url: String) {
        val normalized = normalizeDomainUseCase(url)
        _uiState.update {
            it.copy(
                url = url,
                domain = normalized.normalizedDomain,
                serviceName = if (it.serviceNameEditedByUser) it.serviceName else normalized.suggestedServiceName
            )
        }
    }

    fun onServiceNameChange(value: String) = _uiState.update { it.copy(serviceName = value, serviceNameEditedByUser = true) }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
        if (value.isBlank()) {
            _uiState.update { it.copy(usernameSuggestions = emptyList()) }
            return
        }
        viewModelScope.launch {
            suggestUsernamesUseCase(value).onSuccess { suggestions ->
                _uiState.update { it.copy(usernameSuggestions = suggestions) }
            }
        }
    }
    fun onUsernameMaskChange(value: String) = _uiState.update { it.copy(usernameMask = value) }
    fun onPasswordMaskChange(value: String) = _uiState.update { it.copy(passwordMask = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onMemoChange(value: String) = _uiState.update { it.copy(memo = value) }

    /** Spec 7: reuse-check runs at write time (here, as the user types), never at search time. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
        if (value.isBlank()) {
            _uiState.update { it.copy(showReuseWarning = false) }
            return
        }
        viewModelScope.launch {
            checkPasswordReuseUseCase(value).onSuccess { isReused ->
                _uiState.update { it.copy(showReuseWarning = isReused) }
            }
        }
    }

    fun dismissReuseWarning() = _uiState.update { it.copy(showReuseWarning = false) }

    /** Selecting an autocomplete suggestion copies the username only — never password/mask (spec 8.4). */
    fun applyUsernameSuggestion(username: String) =
        _uiState.update { it.copy(username = username, usernameSuggestions = emptyList()) }

    fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        val now = Instant.now()
        val credential = Credential(
            credentialId = state.credentialId ?: "",
            serviceName = state.serviceName,
            url = state.url.ifBlank { null },
            domain = state.domain.ifBlank { null },
            username = state.username,
            password = state.password,
            usernameMask = state.usernameMask.ifBlank { null },
            passwordMask = state.passwordMask.ifBlank { null },
            category = state.category.ifBlank { null },
            memo = state.memo.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            val result = if (!state.isEditing) {
                createCredentialUseCase(credential)
            } else {
                updateCredentialUseCase(credential.copy(credentialId = state.credentialId!!)).map { state.credentialId }
            }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, saved = true) } },
                onFailure = { e ->
                    val message = e.message?.let { UiText.Dynamic(it) } ?: UiText.of(R.string.error_save_failed)
                    _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                }
            )
        }
    }
}
