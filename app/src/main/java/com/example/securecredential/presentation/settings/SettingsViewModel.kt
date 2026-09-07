package com.example.securecredential.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securecredential.R
import com.example.securecredential.core.util.AppLanguage
import com.example.securecredential.core.util.PinPolicy
import com.example.securecredential.core.util.UiText
import com.example.securecredential.data.preferences.AppPreferences
import com.example.securecredential.data.preferences.ThemeMode
import com.example.securecredential.data.security.KeyLifecycleOrchestrator
import com.example.securecredential.presentation.authentication.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinChangeStep { OLD_PIN, NEW_PIN, NEW_PIN_CONFIRM }

data class SettingsUiState(
    val autoLockTimeoutSeconds: Long = 30,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.DEFAULT,
    val biometricEnabled: Boolean = false,
    val isChangingPin: Boolean = false,
    val pinChangeStep: PinChangeStep = PinChangeStep.OLD_PIN,
    val oldPin: String = "",
    val newPin: String = "",
    val errorMessage: UiText? = null,
    val infoMessage: UiText? = null
)

/** Spec 13.2 Settings sub-sections: Authentication, Auto Lock, Backup/Restore, Security, Theme
 *  (+ Language, added at the user's request — not in the original spec). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val keyLifecycleOrchestrator: KeyLifecycleOrchestrator,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(language = AppLanguage.current()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var pendingNewPin: String? = null

    init {
        appPreferences.autoLockTimeout.onEach { timeout ->
            _uiState.update { it.copy(autoLockTimeoutSeconds = timeout.inWholeSeconds) }
        }.launchIn(viewModelScope)

        appPreferences.themeMode.onEach { mode ->
            _uiState.update { it.copy(themeMode = mode) }
        }.launchIn(viewModelScope)
    }

    fun setAutoLockTimeoutSeconds(seconds: Long) {
        viewModelScope.launch { appPreferences.setAutoLockTimeout(seconds.seconds) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appPreferences.setThemeMode(mode) }
    }

    /** AppCompatDelegate persists this itself (see core/util/AppLanguage) — no DataStore needed. */
    fun setLanguage(language: AppLanguage) {
        AppLanguage.apply(language)
        _uiState.update { it.copy(language = language) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            keyLifecycleOrchestrator.updateBiometricPreference(enabled).fold(
                onSuccess = { _uiState.update { it.copy(biometricEnabled = enabled, infoMessage = null, errorMessage = null) } },
                onFailure = { e -> _uiState.update { it.copy(errorMessage = e.message?.let { m -> UiText.Dynamic(m) }) } }
            )
        }
    }

    fun beginPinChange() =
        _uiState.update { it.copy(isChangingPin = true, pinChangeStep = PinChangeStep.OLD_PIN, oldPin = "", newPin = "", errorMessage = null) }

    fun cancelPinChange() = _uiState.update { it.copy(isChangingPin = false) }

    fun appendPinChangeDigit(digit: Char) = _uiState.update { state ->
        when (state.pinChangeStep) {
            PinChangeStep.OLD_PIN -> state.copy(oldPin = state.oldPin + digit)
            else -> state.copy(newPin = state.newPin + digit)
        }
    }

    fun pinChangeBackspace() = _uiState.update { state ->
        when (state.pinChangeStep) {
            PinChangeStep.OLD_PIN -> state.copy(oldPin = state.oldPin.dropLast(1))
            else -> state.copy(newPin = state.newPin.dropLast(1))
        }
    }

    fun submitOldPin() = _uiState.update { it.copy(pinChangeStep = PinChangeStep.NEW_PIN) }

    fun submitNewPin() {
        val newPin = _uiState.value.newPin
        if (!PinPolicy.isValid(newPin)) {
            _uiState.update { it.copy(errorMessage = UiText.of(R.string.error_pin_too_short, PinPolicy.MIN_NUMERIC_LENGTH)) }
            return
        }
        pendingNewPin = newPin
        _uiState.update { it.copy(newPin = "", pinChangeStep = PinChangeStep.NEW_PIN_CONFIRM, errorMessage = null) }
    }

    fun submitNewPinConfirmation() {
        val state = _uiState.value
        val pending = pendingNewPin
        if (pending == null || pending != state.newPin) {
            _uiState.update {
                it.copy(newPin = "", errorMessage = UiText.of(R.string.error_pin_mismatch), pinChangeStep = PinChangeStep.NEW_PIN)
            }
            return
        }
        viewModelScope.launch {
            keyLifecycleOrchestrator.onPinChange(state.oldPin.toCharArray(), pending.toCharArray()).fold(
                onSuccess = {
                    pendingNewPin = null
                    _uiState.update { it.copy(isChangingPin = false, infoMessage = UiText.of(R.string.info_pin_changed)) }
                },
                onFailure = { e ->
                    val message = e.message?.let { UiText.Dynamic(it) } ?: UiText.of(R.string.error_pin_change_failed)
                    _uiState.update {
                        it.copy(
                            errorMessage = message,
                            pinChangeStep = PinChangeStep.OLD_PIN,
                            oldPin = ""
                        )
                    }
                }
            )
        }
    }

    fun onLockNow() = sessionManager.lock()
}
