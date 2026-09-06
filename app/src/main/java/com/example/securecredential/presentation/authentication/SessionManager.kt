package com.example.securecredential.presentation.authentication

import com.example.securecredential.data.preferences.AppPreferences
import com.example.securecredential.data.security.CryptoManager
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class AppSessionState { LOCKED, AUTHENTICATING, UNLOCKED }
enum class CredentialDisplayState { MASKED, DISPLAYED }

/**
 * Spec section 9.1. The two state machines are deliberately independent: backgrounding always
 * masks a displayed credential immediately, while the app-lock decision only happens when the
 * app is foregrounded again and the configured Auto Lock timeout has elapsed since it went to
 * background — a "check on resume" pattern, since Android does not reliably keep a timer
 * running while the process is backgrounded.
 */
interface SessionManager {
    val appState: StateFlow<AppSessionState>
    val credentialDisplayState: StateFlow<CredentialDisplayState>

    fun beginAuthenticating()
    fun unlock()
    /** Also clears CryptoManager's in-memory keys (spec invariant #6 / section 9.2). */
    fun lock()
    fun displayCredential()
    /** "Security Reset" (spec 9.1/11) — display state only, never touches stored Credential data. */
    fun resetCredentialDisplay()

    /** Call from AppLifecycleObserver when the app has no more visible Activities. */
    fun onAppBackgrounded()
    /** Call from AppLifecycleObserver when the app becomes visible again. */
    suspend fun onAppForegrounded()
}

@Singleton
class SessionManagerImpl @Inject constructor(
    private val cryptoManager: CryptoManager,
    private val appPreferences: AppPreferences,
    private val clock: Clock = Clock.systemUTC()
) : SessionManager {

    private val _appState = MutableStateFlow(AppSessionState.LOCKED)
    override val appState: StateFlow<AppSessionState> = _appState.asStateFlow()

    private val _credentialDisplayState = MutableStateFlow(CredentialDisplayState.MASKED)
    override val credentialDisplayState: StateFlow<CredentialDisplayState> = _credentialDisplayState.asStateFlow()

    private var backgroundedAt: Instant? = null

    override fun beginAuthenticating() {
        _appState.value = AppSessionState.AUTHENTICATING
    }

    override fun unlock() {
        _appState.value = AppSessionState.UNLOCKED
    }

    override fun lock() {
        _appState.value = AppSessionState.LOCKED
        _credentialDisplayState.value = CredentialDisplayState.MASKED
        cryptoManager.clearKeys()
    }

    override fun displayCredential() {
        _credentialDisplayState.value = CredentialDisplayState.DISPLAYED
    }

    override fun resetCredentialDisplay() {
        _credentialDisplayState.value = CredentialDisplayState.MASKED
    }

    override fun onAppBackgrounded() {
        // Independent of the auto-lock timer below: always mask immediately (spec 9.1).
        _credentialDisplayState.value = CredentialDisplayState.MASKED
        backgroundedAt = Instant.now(clock)
    }

    override suspend fun onAppForegrounded() {
        val bgAt = backgroundedAt ?: return
        backgroundedAt = null
        if (_appState.value != AppSessionState.UNLOCKED) return

        val timeoutMillis = appPreferences.autoLockTimeout.first().inWholeMilliseconds
        val elapsedMillis = Duration.between(bgAt, Instant.now(clock)).toMillis()
        if (elapsedMillis >= timeoutMillis) {
            lock()
        }
    }
}
