package com.example.securecredential

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.securecredential.data.preferences.ThemeMode
import com.example.securecredential.presentation.common.ThemeViewModel
import com.example.securecredential.presentation.common.theme.SecureVaultTheme
import com.example.securecredential.presentation.navigation.SecureVaultNavHost
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (not bare ComponentActivity/FragmentActivity) for two reasons:
// 1. androidx.biometric.BiometricPrompt requires a FragmentActivity/Fragment host
//    (data.security.BiometricAuthManager) — AppCompatActivity extends FragmentActivity.
// 2. AppCompatDelegate.setApplicationLocales() (Settings > Language) only actually applies
//    the per-app locale on API < 33 when the hosting Activity is an AppCompatActivity —
//    with a plain FragmentActivity, the locale is stored but never applied to resources.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SecureVaultTheme(useDarkTheme = useDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecureVaultNavHost()
                }
            }
        }
    }
}
