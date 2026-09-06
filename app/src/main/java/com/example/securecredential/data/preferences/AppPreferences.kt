package com.example.securecredential.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = AppPreferencesImpl.DATASTORE_NAME)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Non-sensitive app settings (spec section 1: "DataStore ... Auto Lock, Theme 등 비민감 설정"). */
interface AppPreferences {
    val autoLockTimeout: Flow<Duration>
    suspend fun setAutoLockTimeout(timeout: Duration)

    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

@Singleton
class AppPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppPreferences {

    companion object {
        const val DATASTORE_NAME = "app_preferences"
        val DEFAULT_AUTO_LOCK_TIMEOUT: Duration = 30.seconds
        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
        private val AUTO_LOCK_TIMEOUT_SECONDS = longPreferencesKey("auto_lock_timeout_seconds")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val autoLockTimeout: Flow<Duration> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[AUTO_LOCK_TIMEOUT_SECONDS]?.seconds ?: DEFAULT_AUTO_LOCK_TIMEOUT
    }

    override suspend fun setAutoLockTimeout(timeout: Duration) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[AUTO_LOCK_TIMEOUT_SECONDS] = timeout.inWholeSeconds
        }
    }

    override val themeMode: Flow<ThemeMode> = context.appPreferencesDataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: DEFAULT_THEME_MODE
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferencesDataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
    }
}
