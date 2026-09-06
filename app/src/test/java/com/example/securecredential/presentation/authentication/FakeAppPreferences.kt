package com.example.securecredential.presentation.authentication

import com.example.securecredential.data.preferences.AppPreferences
import com.example.securecredential.data.preferences.ThemeMode
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPreferences(
    initialTimeout: Duration,
    initialTheme: ThemeMode = ThemeMode.SYSTEM
) : AppPreferences {
    private val timeoutState = MutableStateFlow(initialTimeout)
    override val autoLockTimeout = timeoutState

    override suspend fun setAutoLockTimeout(timeout: Duration) {
        timeoutState.value = timeout
    }

    private val themeState = MutableStateFlow(initialTheme)
    override val themeMode = themeState

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeState.value = mode
    }
}
