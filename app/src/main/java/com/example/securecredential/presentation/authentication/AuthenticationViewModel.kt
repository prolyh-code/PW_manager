package com.example.securecredential.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.R
import com.example.securecredential.core.util.UiText
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthenticationUiState(
    val isAuthenticating: Boolean = false,
    val errorMessage: UiText? = null,
    val unlocked: Boolean = false
)

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val keyLifecycleOrchestrator: KeyLifecycleOrchestrator,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState: StateFlow<AuthenticationUiState> = _uiState.asStateFlow()

    init {
        sessionManager.beginAuthenticating()
    }

    /** Cipher to hand to BiometricPrompt as a CryptoObject, pre-authentication. */
    suspend fun prepareCipher(): Result<Cipher> = keyLifecycleOrchestrator.prepareLocalUnlock()

    fun onAuthenticationSucceeded(cipher: Cipher) {
        _uiState.update { it.copy(isAuthenticating = true, errorMessage = null) }
        viewModelScope.launch {
            val result = keyLifecycleOrchestrator.completeLocalUnlock(cipher)
            result.fold(
                onSuccess = {
                    sessionManager.unlock()
                    _uiState.update { it.copy(isAuthenticating = false, unlocked = true) }
                },
                onFailure = { e ->
                    val message = e.message?.let { UiText.Dynamic(it) } ?: UiText.of(R.string.error_unlock_failed_default)
                    _uiState.update { it.copy(isAuthenticating = false, errorMessage = message) }
                }
            )
        }
    }

    fun onAuthenticationFailed(reason: UiText? = null) {
        _uiState.update { it.copy(errorMessage = reason ?: UiText.of(R.string.error_authentication_failed_default)) }
    }
}
