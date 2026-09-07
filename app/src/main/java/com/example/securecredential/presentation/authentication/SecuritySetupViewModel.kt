package com.example.securecredential.presentation.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.R
import com.example.securecredential.core.util.PinPolicy
import com.example.securecredential.core.util.UiText
import com.example.securecredential.data.preferences.AppPreferences
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SecuritySetupStep { BIOMETRIC, PIN_CREATE, PIN_CONFIRM, AUTO_LOCK }

data class SecuritySetupUiState(
    val step: SecuritySetupStep = SecuritySetupStep.BIOMETRIC,
    val biometricEnabled: Boolean = false,
    val pin: String = "",
    val pinConfirmation: String = "",
    val autoLockTimeoutSeconds: Long = 30,
    val isSubmitting: Boolean = false,
    val errorMessage: UiText? = null,
    val completed: Boolean = false
)

/**
 * Spec 13.3 Security Setup flow: Biometric -> PIN (create+confirm) -> Auto Lock -> onFirstLaunch.
 * The keypad only produces digits, so this screen only reaches spec 5.2's numeric-PIN branch
 * (8+ digits) — the 6+ mixed-alphanumeric branch is policy-supported but not yet reachable
 * from any screen; a full keyboard entry mode is deferred (not required for MVP's core loop).
 */
@HiltViewModel
class SecuritySetupViewModel @Inject constructor(
    private val keyLifecycleOrchestrator: KeyLifecycleOrchestrator,
    private val appPreferences: AppPreferences
) : ViewModel() {

    companion object {
        const val MAX_PIN_LENGTH = 12
        const val MIN_NUMERIC_LENGTH = PinPolicy.MIN_NUMERIC_LENGTH
    }

    private val _uiState = MutableStateFlow(SecuritySetupUiState())
    val uiState: StateFlow<SecuritySetupUiState> = _uiState.asStateFlow()

    fun setBiometricEnabled(enabled: Boolean) = _uiState.update { it.copy(biometricEnabled = enabled) }

    fun goToStep(step: SecuritySetupStep) = _uiState.update { it.copy(step = step, errorMessage = null) }

    fun appendDigit(digit: Char) = _uiState.update { state ->
        when (state.step) {
            SecuritySetupStep.PIN_CREATE -> state.copy(pin = (state.pin + digit).take(MAX_PIN_LENGTH))
            SecuritySetupStep.PIN_CONFIRM -> state.copy(pinConfirmation = (state.pinConfirmation + digit).take(MAX_PIN_LENGTH))
            else -> state
        }
    }

    fun backspace() = _uiState.update { state ->
        when (state.step) {
            SecuritySetupStep.PIN_CREATE -> state.copy(pin = state.pin.dropLast(1))
            SecuritySetupStep.PIN_CONFIRM -> state.copy(pinConfirmation = state.pinConfirmation.dropLast(1))
            else -> state
        }
    }

    fun setAutoLockTimeoutSeconds(seconds: Long) = _uiState.update { it.copy(autoLockTimeoutSeconds = seconds) }

    fun confirmPinAndProceed() {
        val pin = _uiState.value.pin
        if (!PinPolicy.isValid(pin)) {
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_pin_too_short, MIN_NUMERIC_LENGTH)) }
            return
        }
        goToStep(SecuritySetupStep.PIN_CONFIRM)
    }

    fun submitPinConfirmationAndProceed() {
        val state = _uiState.value
        if (state.pin != state.pinConfirmation) {
            _uiState.update { it.copy(pinConfirmation = "", errorMessage = UiText.of(R.string.error_pin_mismatch)) }
            return
        }
        goToStep(SecuritySetupStep.AUTO_LOCK)
    }

    fun finishSetup() {
        val state = _uiState.value
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            // PBKDF2 (310,000 iterations) is deliberately CPU-heavy — never run on the main thread.
            val result = withContext(Dispatchers.Default) {
                keyLifecycleOrchestrator.onFirstLaunch(state.pin.toCharArray(), requireBiometric = state.biometricEnabled)
            }
            appPreferences.setAutoLockTimeout(state.autoLockTimeoutSeconds.seconds)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, completed = true) } },
                onFailure = { e ->
                    val message = e.message?.let { UiText.Dynamic(it) } ?: UiText.of(R.string.error_setup_failed)
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
                }
            )
        }
    }
}
