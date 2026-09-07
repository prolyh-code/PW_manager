package com.example.securecredential.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.R
import com.example.securecredential.core.util.PinPolicy
import com.example.securecredential.core.util.UiText
import com.example.securecredential.data.security.CryptoManager
import com.example.securecredential.data.security.KeyLifecycleError
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import com.example.securecredential.data.security.RecoveredKeys
import com.example.securecredential.data.security.RecoveryUnwrapError
import com.example.securecredential.domain.usecase.GetCredentialUseCase
import com.example.securecredential.domain.usecase.ListCredentialsUseCase
import com.example.securecredential.presentation.authentication.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val restorePin: String = "",
    val isRestoring: Boolean = false,
    val restoreComplete: Boolean = false,
    val errorMessage: UiText? = null
)

/**
 * Spec 5.4/10 Restore sequence, now that Room exists (Milestone B) to run a real AC-BACKUP-03
 * decrypt-test: recover keys (PIN-gated) -> temporarily load them into CryptoManager -> decrypt
 * one real stored Credential -> only THEN finalize (regenerate the local Keystore wrap). A
 * failed decrypt-test leaves stored data untouched (AC-BACKUP-07 — no destructive fallback).
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val keyLifecycleOrchestrator: KeyLifecycleOrchestrator,
    private val cryptoManager: CryptoManager,
    private val listCredentialsUseCase: ListCredentialsUseCase,
    private val getCredentialUseCase: GetCredentialUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun appendPinDigit(digit: Char) = _uiState.update { it.copy(restorePin = (it.restorePin + digit).take(PinPolicy.MIN_NUMERIC_LENGTH + 4)) }
    fun backspace() = _uiState.update { it.copy(restorePin = it.restorePin.dropLast(1)) }

    fun submitRestorePin() {
        val pin = _uiState.value.restorePin
        _uiState.update { it.copy(isRestoring = true, errorMessage = null) }
        viewModelScope.launch {
            val recovered = keyLifecycleOrchestrator.beginRestore(pin.toCharArray()).getOrElse { e ->
                _uiState.update { it.copy(isRestoring = false, errorMessage = describeRestoreError(e)) }
                return@launch
            }

            if (!runDecryptTest(recovered)) {
                cryptoManager.clearKeys()
                _uiState.update {
                    it.copy(isRestoring = false, errorMessage = UiText.of(R.string.error_restore_decrypt_failed))
                }
                return@launch
            }

            keyLifecycleOrchestrator.completeRestore(recovered, requireBiometric = false)
            sessionManager.unlock()
            _uiState.update { it.copy(isRestoring = false, restoreComplete = true) }
        }
    }

    /** AC-BACKUP-03: decrypt-test against real stored data, using the recovered (not-yet-final) keys. */
    private suspend fun runDecryptTest(recovered: RecoveredKeys): Boolean {
        cryptoManager.loadKeys(recovered.appEncryptionKey, recovered.searchHmacKey, recovered.idIndexKey)
        val summaries = listCredentialsUseCase().getOrElse { return false }
        if (summaries.isEmpty()) return true // nothing to verify against — an empty backup is not a failure
        return getCredentialUseCase(summaries.first().credentialId).isSuccess
    }

    private fun describeRestoreError(e: Throwable): UiText = when (e) {
        is RecoveryUnwrapError.PinMismatch -> UiText.of(R.string.error_incorrect_pin)
        is RecoveryUnwrapError.AttemptLimited -> UiText.of(R.string.error_too_many_attempts_format, e.retryAfterSeconds)
        is KeyLifecycleError.NotInitialized -> UiText.of(R.string.error_no_backup_data)
        else -> e.message?.let { UiText.Dynamic(it) } ?: UiText.of(R.string.error_restore_failed_default)
    }
}
